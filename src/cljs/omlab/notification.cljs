(ns omlab.notification
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! <! >! chan timeout] :as async]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [omlab.util :refer [guid now] :as util]))

(def ^:const default-ttl 5000) ;show notification for 5s

(defn show-notification [app message & {:keys [ttl type] :or {ttl default-ttl type :info}}]
  (let [guid (guid)]
    (.log js/console "notifo: " message)
    (go
      (om/update! app [:notifications guid] {:msg message :type type})
      (when ttl
        (<! (timeout ttl))
        (om/transact! app [:notifications] #(dissoc % guid))))
    guid))

(defn hide-notification [app guid]
  (om/transact! app [:notifications] #(dissoc % guid)))

(defn notification-panel [app owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (or (:react-name opts) "NotificationPanel"))
    
    om/IRender
    (render [_]
      (let [message (second (first (:notifications app)))
            message-style (when (:type message) (str " ntf-" (name (:type message))))]
        (dom/div #js {:className (str "notification" message-style)}
                 (dom/div nil (:msg message)))))))
