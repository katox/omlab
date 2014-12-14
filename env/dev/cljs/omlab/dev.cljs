(ns omlab.dev
  (:require [omlab.ui :as ui]
            [weasel.repl :as weasel]))

(enable-console-print!)
(weasel/connect "ws://localhost:9001" :verbose true)
(ui/main)
