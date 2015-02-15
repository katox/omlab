(ns omlab.view
  (:require [hiccup.page :as h]
            [environ.core :as environ]
            (ring.middleware [file-info :refer [wrap-file-info]]
                             [content-type :refer [wrap-content-type]]
                             [head :refer [wrap-head]])
            [ring.util.response :as resp]))

(defn main-page-body-dev [req]
  [:body
   [:div {:id "content"}
    [:p "Omlab development version."]]
   [:script {:src "http://fb.me/react-0.12.2.js" :type "text/javascript"}]
   [:script {:src "js/out/goog/base.js" :type "text/javascript"}]
   [:script {:src "js/omlab.js" :type "text/javascript"}]
   [:script {:type "text/javascript"} "goog.require(\"omlab.dev\");"]])

(defn main-page-body-prod [req]
  [:body
    [:div {:id "content"}
     [:p "Welcome to Omlab."]]
   [:script {:src "js/omlab.js" :type "text/javascript"}]])

(defn main-page-body [req]
  (if (environ/env :development)
    (main-page-body-dev req)
    (main-page-body-prod req)))

(defn main-page [req]
  (h/html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:link {:rel "stylesheet"
            :href "https://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css"}]
    [:link {:rel "stylesheet"
            :href "css/omlab.css"}]]
   (main-page-body req)))

(defn signin-page [request]
  (-> (resp/resource-response "public/signin.html")))
