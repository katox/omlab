(defproject omlab "0.1.0-SNAPSHOT"
  :description "Omlab playground"
  :url "https://github.com/katox/omlab"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [hiccup "1.0.5"]
                 [cheshire "5.4.0"]
                 [slingshot "0.12.2"]
                 [environ "1.0.0"]
                 [ring-server "0.4.0" :exclusions [org.clojure/core.incubator]]
                 [ring-basic-authentication "1.0.5"]
                 [com.cemerick/friend "0.2.0" :exclusions [org.clojure/core.cache commons-codec]]
                 [clj-time "0.9.0"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.datomic/datomic-free "0.9.5130" :exclusions [org.clojure/clojure com.google.guava/guava commons-codec joda-time]]
                 [org.clojure/core.match "0.2.2"]
                 [ring-transit "0.1.3"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2814"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [secretary "1.2.1"]
                 [cljs-http "0.1.26" :exclusions [commons-codec]]
                 [org.omcljs/om "0.8.8"]
                 [inflections "0.9.13" :exclusions [commons-codec org.clojure/clojurecommons-codec]]
                 [com.cemerick/piggieback "0.1.5"]
                 [weasel "0.6.0-SNAPSHOT"]
                 ;; TEST
                 [org.clojars.runa/conjure "2.2.0" :scope "test"]
                 [org.clojure/test.generative "0.5.1" :scope "test"]]

  :plugins [[lein-ring "0.9.1"]
            [lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.4"]]
  
  :uberjar-name "omlab-standalone.jar"
  :ring {:handler omlab.handler/app
         :init omlab.handler/init
         :destroy omlab.handler/destroy}

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]

  :cljsbuild {:builds {:app
                       {:source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/omlab.js"
                                   :output-dir "resources/public/js/out"
                                   :source-map "resources/public/js/out.js.map"
                                   :optimizations :none
                                   :pretty-print true
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}}}
  
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [org.clojure/tools.namespace "0.2.9"]
                        [ring-mock "0.1.5"]]
         :source-paths ["env/dev/clj"]
         :env {:config-file "dev-resources/config_dev.edn"
               :development true}
         :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

   :prod {:env {:development false}
          :cljsbuild {:builds {:app {:source-paths ["env/prod/cljs"]
                                     :compiler
                                     {:optimizations :advanced
                                      :pretty-print false}}}}}

   :uberjar {:omit-source true
             :aot :all}})
