(ns omlab.db.user
  (:import java.util.concurrent.ExecutionException)
  (:require [omlab.util :refer [ns-keyword has-keys?] :as util]
            (omlab.db [util :as db.util]
                         [query :as db.query])
            [slingshot.slingshot :refer [try+ throw+]]
            [datomic.api :refer (q db) :as d]))

(defn find-entity-id [db username]
  (when username
    (ffirst (q '[:find ?u
                 :in $ ?username
                 :where
                 [?u :user/username ?username]]
               db username))))

(defn auth-credentials [db login]
  (when-let [[login pass roles name]
             (first
              (q '[:find ?username ?pass (set ?roles) ?name
                   :in $ % ?username
                   :where
                   [?u :user/username ?username]
                   [?u :user/password ?pass]
                   [?u :user/name ?name]
                   (ref-keyword ?u :user/roles ?roles)]
                 db db.query/ref-rules login))]
    {:username login :password pass :roles roles :name name}))

(defn list-users [db]
  (->> (q '[:find ?user ?email (set ?roles) ?type ?name
            :in $ %
            :where
            [?u :user/username ?user]
            [?u :user/name ?name]
            [?u :user/email ?email]
            (ref-keyword ?u :user/type ?type)
            (ref-keyword ?u :user/roles ?roles)]
          db db.query/ref-rules)
       (util/remove-ns-from-keywords)
       (sort-by first)
       (mapv (partial zipmap [:username :email :roles :type :name]))))

(defn user-detail [db username]
  {:pre [(not (nil? username))]}
  (when-let [eid (find-entity-id db username)]
    (some->
     (db.util/get-entity-map db eid)
     (dissoc :db/id :user/password)
     (assoc :mod-time (db.query/mod-time db eid))
     (util/remove-ns-from-keywords))))

(defn add-user [conn auth-ctx user]
  {:pre [(and (has-keys? user :name :type :roles :email :username)
              (not (empty? (:roles user))))]}
  (if-not (find-entity-id (d/db conn) (:username user))
    (let [eid (d/tempid :db.part/user)]
      (try
        @(db.util/atransact-all conn auth-ctx
                                [{:db/id eid
                                  :user/username (:username user)
                                  :user/password (:password user)
                                  :user/email (:email user)
                                  :user/roles (set (map #(ns-keyword "omlab.auth" %) (:roles user)))
                                  :user/name (:name user)
                                  :user/type (ns-keyword "user.type" (:type user))}
                                 [:db.fn/cas eid :user/optlock nil 0]])
        (catch ExecutionException e
          (throw+ {:type :optlock-conflict :entity :user :username (:username user) :msg (.getMessage e)}))))
    (throw+ {:type :already-exists :entity :user :username (:username user)})))

(defn update-user [conn auth-ctx user]
  {:pre [(and (has-keys? user :username :optlock)
              (or (nil? (:roles user)) (not (empty? (:roles user)))))]}
  (let [eid (find-entity-id (d/db conn) (:username user))
        optlock (:optlock user)
        valid-roles (set (map #(ns-keyword "omlab.auth" %) (:roles user)))
        retract-roles (when (:roles user)
                        (->> (q '[:find ?op ?e ?a ?v
                                  :in $ ?op ?e ?ea
                                  :where [?e ?ea ?en]
                                         [?ea :db/ident ?a]
                                         [?en :db/ident ?v]]
                                (d/db conn) :db/retract eid (d/entid (d/db conn) :user/roles))
                             (remove (fn [[_ _ _ role]] (contains? valid-roles role)))))]
    (try
      @(db.util/atransact-all conn auth-ctx
                              (concat
                               [{:db/id eid
                                 :user/username (:username user)
                                 :user/password (:password user)
                                 :user/email (:email user)
                                 :user/roles valid-roles
                                 :user/name (:name user)
                                 :user/type (ns-keyword "user.type" (:type user))}
                                [:db.fn/cas eid :user/optlock optlock ((fnil inc -1) optlock)]]
                               retract-roles))
      (catch ExecutionException e
        (throw+ {:type :optlock-conflict :entity :user :username (:username user) :msg (.getMessage e)})))))
