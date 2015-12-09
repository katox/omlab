(defproject omlab "0.1.0-SNAPSHOT"
  :description "Omlab playground"
  :url "https://github.com/katox/omlab"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.5.0"]
                 [slingshot "0.12.2"]
                 [environ "1.0.1"]
                 [ring-server "0.4.0" :exclusions [org.clojure/core.incubator]]
                 [ring-basic-authentication "1.0.5"]
                 [com.cemerick/friend "0.2.0" :exclusions [org.clojure/core.cache commons-codec]]
                 [clj-time "0.11.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [com.datomic/datomic-free "0.9.5344" :exclusions [org.clojure/clojure com.google.guava/guava commons-codec joda-time]]
                 [org.clojure/core.match "0.2.2"]
                 [ring-transit "0.1.4"]
                 ;; CLJS
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.7.170"]
                 [secretary "1.2.3"]
                 [cljs-http "0.1.38" :exclusions [commons-codec com.cemerick/austin]]
                 [org.omcljs/om "0.9.0"]
                 [inflections "0.10.0" :exclusions [org.clojure/clojure commons-codec]]
                 ;; TEST
                 [org.clojars.runa/conjure "2.2.0" :scope "test"]
                 [org.clojure/test.generative "0.5.2" :scope "test"]]

  :plugins [[lein-ring "0.9.1"]
            [lein-environ "1.0.0"]
            [lein-cljsbuild "1.1.1"]]
  
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
  :clean-targets ^{:protect false} ["target", "resources/public/js/"]
  
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [org.clojure/tools.namespace "0.2.10"]
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
