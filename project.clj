(defproject omlab "0.1.0-SNAPSHOT"
  :description "Omlab playground"
  :url "https://github.com/katox/omlab"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [hiccup "1.0.5"]
                 [cheshire "5.3.1"]
                 [slingshot "0.10.3"]
                 [environ "0.5.0"] 
                 [ring-server "0.3.1" :exclusions [org.clojure/core.incubator]]
                 [ring-basic-authentication "1.0.5"]
                 [fogus/ring-edn "0.2.0"]
                 [com.cemerick/friend "0.2.0" :exclusions [org.clojure/core.cache commons-codec]]
                 [clj-time "0.8.0"]
                 [com.taoensso/timbre "3.2.1"]
                 [com.datomic/datomic-free "0.9.4880.2" :exclusions [org.clojure/clojure com.google.guava/guava commons-codec]]
                 [org.clojure/core.match "0.2.1"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2277"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [secretary "1.2.0"]
                 [cljs-http "0.1.15"]
                 [om "0.7.1"]
                 [inflections "0.9.9" :exclusions [org.clojure/clojurecommons-codec]]
                 ;; TEST
                 [org.clojars.runa/conjure "2.2.0" :scope "test"]
                 [org.clojure/test.generative "0.5.1" :scope "test"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-environ "0.5.0"]
            [lein-cljsbuild "1.0.3"]]
  
  :uberjar-name "omlab-standalone.jar"
  :ring {:handler omlab.handler/app
         :init omlab.handler/init
         :destroy omlab.handler/destroy}

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]

  :cljsbuild {:builds {:prod
                       {:source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/omlab.js"
                                   :output-dir "target/cljs-tmp"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}}}
  
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [org.clojure/tools.namespace "0.2.5"]
                        [ring-mock "0.1.5"]]
         :source-paths ["dev"]
         ;:plugins [[com.cemerick/austin "0.1.4"]]
         :env {:config-file "dev-resources/config_dev.edn"
               :development true}
         :injections [(user/init)]
         :cljsbuild {:builds
                     {:none
                      {:source-paths ["src/cljs"]
                       :compiler {:output-to "dev-resources/public/js/dev/omlab.js"
                                  :output-dir "dev-resources/public/js/dev"
                                  :source-map true
                                  :optimizations :none
                                  :pretty-print true
                                  :externs ["react/externs/react.js"]}}
                      :whitespace
                      {:source-paths ["src/cljs"]
                       :compiler {:output-to "dev-resources/public/js/whitespace/omlab.js"
                                  :output-dir "dev-resources/public/js/whitespace"
                                  :source-map "dev-resources/public/js/whitespace/omlab.js.map"
                                  :optimizations :whitespace
                                  :pretty-print true
                                  :externs ["react/externs/react.js"]}}
                      :simple
                      {:source-paths ["src/cljs"]
                       :compiler {:output-to "dev-resources/public/js/simple/omlab.js"
                                  :output-dir "dev-resources/public/js/simple"
                                  :source-map "dev-resources/public/js/simple/omlab.js.map"
                                  :optimizations :simple
                                  :pretty-print true
                                  :externs ["react/externs/react.js"]}}
                      :advanced
                      {:source-paths ["src/cljs"]
                       :compiler {:output-to "dev-resources/public/js/advanced/omlab.js"
                                  :output-dir "dev-resources/public/js/advanced"
                                  :source-map "dev-resources/public/js/advanced/omlab.js.map"
                                  :optimizations :advanced
                                  :pretty-print true
                                  :externs ["react/externs/react.js"]}}}}}
   :prod {:env {:development false}}})
