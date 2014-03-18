(ns omlab.db.migration
  (:require [omlab.util :as util]
            [omlab.db.schema :as schema]
            [datomic.api :as d]
            [clojure.java.io :as io]))

(def migrations
  [:000-audit-schema
   :001-user-schema
   :002-user-seed-data
   ])

(defn- migration-filename [kname]
  (str "dtm/" (name kname) ".edn"))

(defn- migration-txes [kname]
  {:txes (util/read-edn-resource (migration-filename kname))})

(defn- migration-schema-map [kname]
  {kname (migration-txes kname)})

(defn- schema-map [selected-migrations]
  (into {} (map migration-schema-map selected-migrations)))

(defn migrate [db-uri]
  {:pre [db-uri]}
  (d/create-database db-uri)
  (let [conn (d/connect db-uri)
        schema (schema-map migrations)]
    (apply schema/ensure-schemas conn :omlab/schema schema migrations)))
