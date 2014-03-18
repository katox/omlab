(ns omlab.dom
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [omlab.handler :refer [update-value!] :as handler]))

(defn glyph-button [button-style glyphicon text]
  (dom/button button-style
              (dom/span #js {:className (str "glyphicon " glyphicon)})
              (str " " text)))

(defn glyph-button-link [link-opts glyphicon text]
  (dom/a link-opts
              (dom/span #js {:className (str "glyphicon " glyphicon)}) (str " " text)))

(defn glyph-button-link-standalone [link-opts glyphicon text & {:keys [disabled]}]
  (dom/div #js {:className "btn-group"}
           (glyph-button-link link-opts glyphicon text)))

(defn profile-img [{:keys [email-hash] :as change} & {:keys [size] :as opts}]
  (let [class-name (case size :small "thumbnail-image-sm" :big "thumbnail-image-big" "thumbnail-image")
        img-size (case size :small 20 :big 100 60)]
    (dom/div #js {:className "profile-img"}
             (dom/div #js {:className "thumbnail-link"}
                      (dom/img #js {:className class-name
                                    :src (str "https://www.gravatar.com/avatar/" email-hash "?s=" img-size "&d=mm")})))))

(defn omlab-label [label-text value]
  (dom/li nil
          (dom/span #js {:className "omlab-label"} label-text) value))

(defn editable-label [description cursor key & {:keys [on-change-fn]}]
  (let [on-change (or on-change-fn (update-value! cursor key))]
    (dom/li nil
            (dom/label #js {:className "omlab-label"}
                       (dom/span #js {:className "omlab-label"} description)
                       (dom/input #js {:className "form-control input-medium"
                                       :type "text" :value (key cursor)
                                       :onChange on-change})))))

(defn editable-set-label [description cursor key state & {:keys [on-change-fn]}]
  (let [on-change (or on-change-fn (update-value! cursor key))]
    (dom/label #js {:className "omlab-label"}
               (dom/span #js {:className "omlab-label"} description)
               (dom/input #js {:className "form-control input-medium"
                               :type "text" :value (:roles state)
                               :onChange on-change}))))
