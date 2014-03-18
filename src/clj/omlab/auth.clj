(ns omlab.auth
  (:require [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [ring.util.response :as resp]
            [hiccup.page :as h]
            [omlab.system :as system]
            (omlab.db [user :as db.user]
                         [util :as db.util])))

(derive ::admin ::user)

(defn auth-credentials [login]
  (db.user/auth-credentials (db.util/get-db @system/settings) login))

;; truthy allow-anon? means "might be authenticated"
;; force authentication with friend/authenticated (or friend/wrap-authorize) guard
;; false value means that authentication handler aborts processing of remaining routes

(defn basic-auth [handler]
  (friend/authenticate
   handler
   {:allow-anon? true
    :unauthenticated-handler (partial workflows/http-basic-deny "Omlab")
    :workflows [(workflows/http-basic
                 :credential-fn (partial creds/bcrypt-credential-fn #(auth-credentials %))
                 :realm "Omlab")]}))

(defn form-auth [handler]
  (friend/authenticate
   handler
   {:allow-anon? true
    :default-landing-uri "/"
    :login-uri "/login"
    :unauthorized-handler
    #(-> (h/html5
          [:h2 "You do not have sufficient privileges to access " (:uri %)])
         resp/response
         (resp/status 401))
    :credential-fn (partial creds/bcrypt-credential-fn #(auth-credentials %))
    :workflows [(workflows/interactive-form)]}))

(defn valid-current-password? [password]
  (let [username (:username (friend/current-authentication))
        creds (auth-credentials username)
        password-key (or (-> creds meta ::password-key) :password)
        current-hash (get creds password-key)]
    (creds/bcrypt-verify password current-hash)))
