(ns omlab.handler
  (:require [compojure.core :refer (GET POST PUT defroutes routes) :as compojure]
            [compojure.handler :as handler]
            [compojure.route :as route]
            (ring.middleware [multipart-params :as mp]
                             [transit :as transit])
            [ring.util.response :as resp]
            [cemerick.friend :as friend]
            [cemerick.friend.credentials :as creds]
            [taoensso.timbre :refer [debug info error]]
            [slingshot.slingshot :refer [try+ throw+]]
            [environ.core :as environ]
            [clj-stacktrace.repl :as stacktrace]
            (omlab [util :refer [to-uuid] :as util]
                   [system :as system]
                   [auth :as auth]
                   [view :as view])
            (omlab.db [util :as db.util]
                      [migration :as db.migration]
                      [user :as db.user])))

(defn init []
  (info "omlab is starting")
  (reset! system/settings (util/read-edn-resource (:config-file environ/env "config.edn")))
  (db.migration/migrate (db.util/get-db-uri @system/settings)))

(defn destroy []
  (info "omlab is shutting down"))

(defn conn []
  (db.util/get-conn @system/settings))

(defn db []
  (db.util/get-db @system/settings))

(defn wrap-conflicts [handler]
  (fn [req]
    (try+
      (handler req)
      (catch [:type :optlock-conflict] {:keys [entity] :as err}
              (-> (resp/response {:msg (str "Update conflict for " (name entity) ". Please refresh and try again.") :type :optlock-conflict :err err})
                  (resp/status 409)))
      (catch [:type :already-exists] {:keys [entity] :as err}
              (-> (resp/response {:msg (str "Record already exists for " (name entity) ".") :type :optlock-conflict :err err})
                  (resp/status 409))))))

(defn wrap-log-stacktrace
  "Wrap a handler such that exceptions are caught and logged
   response is returned."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable ex
        (error "Request: " request ". Exception:" (with-out-str (stacktrace/pst ex)))
        (throw ex)))))

(defroutes ui-routes
  (GET "/current-user" req
       (resp/response (-> (friend/current-authentication)
                         (select-keys [:username :name :roles])
                         (util/remove-ns-from-keywords))))
  (GET "/profile" req
       (resp/response (db.user/user-detail (db) (:username (friend/current-authentication)))))
  (POST "/profile" {profile :params}
        (do
          (let [username (:username (friend/current-authentication))
                password (when (:password profile) (creds/hash-bcrypt (:password profile)))
                actual-profile (-> profile
                                   (select-keys [:name :email :password :optlock])
                                   (assoc :username username :password password))]
            (info "Updating profile for user " username ":" (pr-str actual-profile))
            (if (auth/valid-current-password? (:old-password profile))
              (-> (conn)
                  (db.user/update-user (friend/current-authentication) actual-profile)
                  (:db-after)
                  (db.user/user-detail username)
                  (resp/response))
              (-> (str "Bad password for profile update for " username)
                  resp/response
                  (resp/status 403)))))))

(defroutes admin-routes
  (GET "/users" req
       (resp/response (db.user/list-users (db))))
  (GET "/user/:username" [username]
       (resp/response (db.user/user-detail (db) username)))
  (POST "/user/:username" {{:keys [username] :as user} :params}
        (do
          (let [password (when (:password user) (creds/hash-bcrypt (:password user)))
                actual-user (-> user
                                (select-keys [:username :name :email :type :roles :optlock])
                                (assoc :password password))]
            (info "Updating user " username ":" (pr-str actual-user))
            (-> (conn)
                (db.user/update-user (friend/current-authentication) actual-user)
                (:db-after)
                (db.user/user-detail username)
                (resp/response)))))
  (PUT "/user/:username" {{:keys [username] :as user} :params}
        (do
          (let [hashed-password (creds/hash-bcrypt (:password user))
                actual-user (if (:password user) (assoc user :password hashed-password) user)]
            (info "Adding user " username ":" (pr-str actual-user))
            (-> (conn)
                (db.user/add-user (friend/current-authentication) actual-user)
                (:db-after)
                (db.user/user-detail username)
                (resp/response))))))

(defroutes ui*
  (compojure/context "/ui" req
                     (friend/wrap-authorize
                      ui-routes #{:omlab.auth/user})))
(defroutes admin-ui*
  (compojure/context "/admin" req
                     (friend/wrap-authorize
                      admin-routes #{:omlab.auth/admin})))

(defroutes app*
  (GET "/login" req
       (view/signin-page req))
  (GET "/logout" req
       (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/" req
       (friend/authorize #{:omlab.auth/user}
                         (view/main-page req))))

(defroutes public-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes (auth/form-auth app*)
              (auth/basic-auth ui*)
              (auth/basic-auth admin-ui*)
              public-routes)
      wrap-log-stacktrace
      wrap-conflicts
      transit/wrap-transit-params
      transit/wrap-transit-response
      handler/site))
