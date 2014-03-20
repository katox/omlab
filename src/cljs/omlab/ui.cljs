(ns omlab.ui
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! >! chan timeout] :as async]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [cljs-http.client :as http]
            [clojure.string :as string]
            [clojure.browser.repl]
            [omlab.util :refer [guid now] :as util]
            [omlab.http :as xhr :include-macros true :refer [go-fetch]]
            [omlab.format :refer [as-str] :as format]
            [omlab.handler :as handler]
            [omlab.dom :as ldom]
            [omlab.navbar :as navbar]
            [omlab.user :as user]
            [omlab.notification :refer [show-notification hide-notification] :as notification])
  (:import [goog History]
           [goog.history EventType]
           [goog.date Date]))

;; Lets you do (prn "stuff") to the console -- don't use with core.async
(enable-console-print!)

(def app-state
  (atom {:showing-tab :admin
         :notifications (array-map)
         :navbar-menu [{:text "Admin" :tab "admin/users" :roles #{:admin}}]
         :users []
         :admin {}
         :new-user {}
         :user-profile {}}))


;; routing
(defroute "/:tab" {:keys [tab]} (swap! app-state assoc :showing-tab (keyword tab)))

(defroute "/admin/:section/add" {:keys [section]} (swap! app-state assoc :showing-tab :admin :admin {:section section :action :add }))

(defroute "/admin/users/show/:username" {:keys [username]} (swap! app-state assoc :showing-tab :admin :admin {:section :users :action :show :showing-user username}))

(defroute "/admin/:section" {:keys [section]} (swap! app-state assoc :showing-tab :admin :admin {:section (keyword section) :action :list}))

(defroute "/" {:as params} (secretary/dispatch! "/admin/users"))

(def history (History.))

(events/listen history EventType.NAVIGATE
  (fn [e]
    (let [part (if-not (empty? (.-token e)) (.-token e) "/")]
      (secretary/dispatch! part))))

(.setEnabled history true)

(defn fetch-current-user [app url]
  (go-fetch
   [{status :status user :body} (<! (http/get url {:timeout xhr/default-timeout}))]
   (util/update-ids user)
   (show-notification app (str "Fetching current user failed. Status code: " status) :type :error)))

(defn footer [app]
  (reify
    om/IDisplayName
    (display-name [_] "Footer")

    om/IRender
    (render [_]
      (dom/footer #js {:className "footer"} "© 2014 leafclick s. r. o."))))

(defn omlab-app [app owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (or (:react-name opts) "OmlabApp"))
    
    om/IInitState
    (init-state [_]
      {:comm (chan)})

    om/IWillMount
    (will-mount [_]
      (let [comm (om/get-state owner :comm)]
        (go (let [user (<! (fetch-current-user app "/ui/current-user"))]
              (om/transact! app #(assoc % :current-user user))))
        (go (while true
              (let [[type value] (<! comm)]
                (handler/handle-app-event type app value))))))
    om/IRenderState
    (render-state [_ {:keys [comm]}]
      (let [comm-init {:init-state {:comm comm}}]
        (dom/div #js {:className "container"}
                 (om/build navbar/navbar app comm-init)
                 (om/build notification/notification-panel app comm-init)
                 (condp = (:showing-tab app)
                   :admin (om/build user/admin app comm-init)
                   :profile (om/build user/profile-edit app comm-init))
                 (om/build footer app comm-init))))))

(om/root omlab-app app-state
         {:target (.getElementById js/document "content")})
