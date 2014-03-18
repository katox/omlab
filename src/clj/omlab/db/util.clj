(ns omlab.db.util
  (:require [datomic.api :refer [q] :as d]
            [cemerick.friend :as friend]
            [omlab.util :as util]))

(defn get-db-uri [settings]
  (:datomic-uri settings))

(defn get-conn [settings]
  {:post [%]}
  (d/connect (get-db-uri settings)))

(defn get-db [settings]
  (d/db (get-conn settings)))

(defn atransact [conn auth-ctx txes]
  "d/transact which automatically adds current authenticated user to tx"
  (if-not auth-ctx
    (throw (ex-info "No authenticated user! Can't process the transaction."
                    {:friend/current-authentication :no-auth})))
  (let [audit-record [:db/add (d/tempid :db.part/tx)
                      :audit/user (:username auth-ctx)]]
    (d/transact conn (conj txes audit-record))))

(defn atransact-all [conn auth-ctx txes]
  "atransact all individual transactions"
  (atransact conn auth-ctx (map util/remove-nils-from-map txes)))

(defn get-entity-map
  "Get a map of the entity's attributes"
  [db id]
  (let [entity (d/entity db id)]
    (reduce #(assoc %1 %2 (%2 entity))
           {:db/id id}
           (keys entity))))

(defn entity-by-attribute
  "Get an entity map by attribute"
  [db attribute value]
  (let [results (q '[:find ?e
                     :in $ ?attribute ?name
                     :where
                     [?e ?attribute ?name]]
                   db attribute value)]
    (case (count results)
      0 nil
      1 (get-entity-map db (ffirst results))
      :else (throw
             (ex-info (str (count results) " entities with " attribute " of " value)
                      {:schema/too-many-entities attribute})))))
