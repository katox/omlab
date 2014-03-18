(ns omlab.http
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs-http.client :as http]))

;; 30s request timeouts
(def ^:const default-timeout 30000)
