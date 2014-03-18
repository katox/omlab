(ns omlab.format
  (:require [clojure.string :as string]
            [om.core :as om :include-macros true]))

(defn as-str [val]
  (cond
   (om/cursor? val) (as-str (om/value val))
   (keyword? val) (name val)
   (instance? js/Date val) (pr-str val)
   :else val))

(defn parse-roles [roles]
  (if roles
    (set (map (comp keyword string/trim) (string/split roles #"\s")))
    #{}))

(defn format-roles [roles]
  (some->> roles
           (map name)
           (interpose " ")
           (apply str)))

