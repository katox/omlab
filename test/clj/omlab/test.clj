(ns omlab.test
  (:require [clojure.test]
            [datomic.api :as d]
            [omlab.util :as util]
            [omlab.db.util :as db.util]
            [omlab.db.migration :as db.migration]
            [omlab.system :as system]))

(defmacro with-private-fns [[ns fns] & tests]
  "Refers private fns from ns and runs tests in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
     ~@tests))

(defn settings-fixture [f]
  (with-redefs [system/settings (atom {:datomic-uri "datomic:mem://omlab-test-db"})]
    (f)))

(defn get-fresh-conn! [data-file settings]
  "Returns new connection to fresh db with data-file loaded."
  (let [uri (db.util/get-db-uri settings)]
    (d/delete-database uri)
    (d/create-database uri)
    (db.migration/migrate uri)
    (let [c (d/connect uri)]
      (d/transact c (util/read-edn-resource "dtm/user-test-data.edn"))
      c)))

(defn refresh-db-fixture [data-file]
  (fn [f]
    (with-redefs [db.util/get-conn #(get-fresh-conn! data-file %)]
      (f))))

(defn db []
  (db.util/get-db @system/settings))

(defn conn []
  (db.util/get-conn @system/settings))
