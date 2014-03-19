(ns omlab.navbar
  (:require [clojure.set :as set]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn navbar-header [app]
  (dom/div #js {:className "navbar-header"}
           (dom/button #js {:type "button" :className "navbar-toggle"
                            :data-toggle "collapse" :data-target ".navbar-collapse"}
                       (dom/span #js {:className "sr-only"} "Toggle navigation")
                       (dom/span #js {:className "icon-bar"})
                       (dom/span #js {:className "icon-bar"})
                       (dom/span #js {:className "icon-bar"}))
           (dom/a #js {:href "#" :className "navbar-brand"} "Omlab")))

(defn navbar-signed-in [app]
  (dom/p #js {:className "navbar-text navbar-right"}
         "Signed in as "
         (dom/a #js {:href "#/profile"} (get-in app [:current-user :name]))
         " | "
         (dom/a #js {:href "/logout"} "Logout")))

(defn navbar-menuitem [{:keys [text tab roles]} showing-tab current-roles]
  (when (seq (set/intersection current-roles roles))
    (dom/li (when (= tab showing-tab) #js {:className "active"})
            (dom/a #js {:href (str "#/" tab)} text))))

(defn navbar-menu [app]
  (dom/div #js {:className "collapse navbar-collapse"}
           (apply dom/ul #js {:className "nav navbar-nav"}
                  (remove nil?
                          (map #(navbar-menuitem % (:showing-tab app) (get-in app [:current-user :roles]))
                               (:navbar-menu app))))))

(defn navbar [app owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (or (:react-name opts) "Navbar"))
    
    om/IRender
    (render [_]
      (dom/div #js {:className "navbar navbar-default"}
               (navbar-header app)
               (navbar-signed-in app)
               (navbar-menu app)))))
