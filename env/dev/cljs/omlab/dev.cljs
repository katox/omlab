(ns omlab.dev
  (:require [omlab.ui :as ui]
            [clojure.browser.repl :as repl]))

(enable-console-print!)
(repl/connect "http://localhost:9000/repl")
(ui/main)
