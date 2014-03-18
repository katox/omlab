(ns omlab.handler
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [omlab.util :refer [guid now] :as util]
            [omlab.format :refer [as-str] :as format]))

(defn update-value!
  ([app key]
     (fn [e]
       (let [nval (.. e -target -value)]
         (om/update! app key nval))))
  ([app key f]
     (fn [e]
       (let [nval (f (.. e -target -value))]
         (om/update! app key nval)))))

(defn handle-roles [e user owner state]
  (let [val (.. e -target -value)
        roles (format/parse-roles val)]
    (when roles
      (om/update! user :roles roles))
    (om/set-state! owner :roles val)))

(defn handle-email [e user owner state]
  (let [nval (.. e -target -value)]
    (om/update! user :email nval)
    (om/update! user :email-hash (util/email-digest nval))))

(defmulti handle-app-event (fn [type _ _] type))
(defmethod handle-app-event :default [type app val]
  (.log js/console "Error: Unknown application event handler type " type "."))
