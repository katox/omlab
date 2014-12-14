(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint pp)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [ring.server.standalone :refer [serve]]
   [cemerick.piggieback]
   [weasel.repl.websocket]
   [datomic.api :as d]
   [omlab.system]
   [omlab.util]
   [omlab.handler]
   ))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn recreate-db
  ([] (recreate-db (:datomic-uri @omlab.system/settings)))
  ([uri]
     (d/delete-database uri)
     (d/create-database uri)
     (omlab.db.migration/migrate uri)))

(defn load-test-data
  ([] (load-test-data (:datomic-uri @omlab.system/settings)))
  ([uri]
     (let [c (d/connect uri)]
       @(d/transact c (omlab.util/read-edn-resource "dtm/user-test-data.edn"))
       c)))

(defn conn
  ([] (conn (:datomic-uri @omlab.system/settings)))
  ([uri] (d/connect uri)))

(defn cljs-repl
  "needs browser tab refresh"
  []
  (cemerick.piggieback/cljs-repl
   :repl-env (weasel.repl.websocket/repl-env
              :ip "0.0.0.0" :port 9001)))

(defn start-server
  "used for starting the server in development mode from REPL"
  [& {:keys [port auto-reload]
      :or {port 4000, auto-reload true}
      :as req}]
  (reset! omlab.system/server
          (serve (-> #'omlab.handler/app
                     ring.middleware.stacktrace/wrap-stacktrace)
                 {:port port
                  :init omlab.handler/init
                  :auto-reload? true
                  :destroy omlab.handler/destroy
                  :join true})))

(defn stop-server []
  (.stop @omlab.system/server)
  (reset! omlab.system/server nil))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (start-server))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (stop-server))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))
