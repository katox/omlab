(ns omlab.util
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as w]
            [clojure.string :as string]
            [slingshot.slingshot :refer [try+ throw+]]
            [omlab.system :as system])
  (:import [java.io PushbackReader StringReader File]
           [java.nio ByteBuffer]
           [java.text SimpleDateFormat]
           [java.util UUID]))

(defn read-edn-uri [uri]
  (with-open [infile (PushbackReader. (io/reader uri))]
    (edn/read {:readers *data-readers*} infile)))

(defn read-edn-resource [path]
  (if-let [url (io/resource path)]
    (read-edn-uri url)
    (read-edn-uri path)))

(defn parse-edn [s]
  (with-open [in (PushbackReader. (StringReader. s))]
    (edn/read {:readers *data-readers*} in)))

(defn remove-nils-from-map  [record]
  (if (map? record)
    (into {} (remove (comp nil? second) record))
    record))

(defn remove-ns-from-keywords [coll]
  (w/postwalk (fn [item] (if (and (keyword? item) (namespace item))
                    (keyword (name item))
                    item))
              coll))

(defn ns-keyword [ns k]
  (if (or (nil? k) (namespace k))
    k
    (keyword ns (name k))))

(defn has-keys? [m & ks]
  (every? true? (map (partial contains? m) ks)))

(defn to-camel-case [s]
  (string/replace s #"-(\w)" #(string/upper-case (second %1))))

(defn remove-from-end [s end]
  (let [i (.lastIndexOf s end)]
    (if (pos? i) (subs s 0 i) s)))

(defn format-date [pattern date]
  (.format (SimpleDateFormat. pattern) date))

(defn is-uuid? [o]
  (instance? java.util.UUID o))

(defn to-uuid [s]
  (if (is-uuid? s)
    s
    (UUID/fromString s)))
