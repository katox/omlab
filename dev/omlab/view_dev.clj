(ns omlab.view-dev
  (:require [hiccup.page :as h]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))

(defn main-page-body [req]
  [:body
    [:div {:id "content"}
     [:p "Omlab development version."]]
    [:script {:src "http://fb.me/react-0.9.0.js" :type "text/javascript"}]
    [:script {:src "js/dev/goog/base.js" :type "text/javascript"}]
    [:script {:src "js/dev/omlab.js" :type "text/javascript"}]
    [:script {:type "text/javascript"} "goog.require(\"omlab.ui\");"]
    [:script (browser-connected-repl-js)]])
