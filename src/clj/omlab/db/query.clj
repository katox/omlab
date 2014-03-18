;; Taken from day of datomic:
;; https://github.com/Datomic/day-of-datomic/blob/master/src/datomic/samples/schema.clj
(ns omlab.db.query
  (:require [datomic.api :as d]
            [omlab.db.schema :as schema]))

(defn maybe
  "Returns the value of attr for e, or if-not if e does not possess
   any values for attr. Cardinality-many attributes will be
   returned as a set"
  [db e attr if-not]
  (let [result (d/q '[:find ?v
                      :in $ ?e ?a
                      :where [?e ?a ?v]]
                    db e attr)]
    (if (seq result)
      (case (schema/cardinality db attr)
        :db.cardinality/one (ffirst result)
        :db.cardinality/many (into #{} (map first result)))
      if-not)))

(defn ref-keyword
  "Returns db/ident of given attr or e or nil if missing."
  [db e attr]
  (when-let [result (seq (d/q '[:find ?v
                          :in $ ?e ?a
                          :where
                          [?e ?a ?en]
                          [?en :db/ident ?v]]
                              db e attr))]
    (-> result ffirst name keyword)))

(def ref-rules
  '[[[ref-keyword ?e ?a ?v]
     [?e ?a ?en] [?en :db/ident ?v]]
    [[tx-auth-name ?tx ?real-name]
     [?tx :audit/user ?u] [?ue :user/username ?u] [?ue :user/name ?real-name]]
    ])

(defn attr-auth-name
  "Returns the real name of user who is in audit/user record in tx of that attr.
   If attr is not present nil is returned."
  [db e attr]
  (ffirst
   (d/q '[:find ?name
          :in $ % ?e ?a
          :where
          [?e ?a _ ?tx]
          (tx-auth-name ?tx ?name)]
        db ref-rules e attr)))

(defn mod-time
  "Returns the last modification time of entity e"
  [db e]
  (some->>
   (d/q '[:find ?inst
          :in $ ?e
          :where
          [?e _ _ ?tx]
          [?tx :db/txInstant ?inst]]
        db e)
   (sort #(compare %2 %1))
   ffirst))
