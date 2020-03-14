(defproject com.left-over/website "0.1.0-SNAPSHOT"
  :description "Left Over website"
  :url "https://www.github.com/skuttleman/left-over.com"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main com.left-over.api.server
  :aot [com.left-over.api.server com.left-over.api.services.db.migrations]
  :min-lein-version "2.9.1"
  :dependencies [[bidi "2.1.3" :exclusions [[ring/ring-core]]]
                 [camel-snake-kebab "0.4.1"]
                 [clj-http "3.9.1"]
                 [clj-jwt "0.1.1"]
                 [cljs-http "0.1.46"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3" :exclusions [[org.clojure/java.jdbc]]]
                 [com.ben-allred/collaj "0.8.0"]
                 [com.ben-allred/formation "0.6.2"]
                 [com.ben-allred/vow "0.1.0"]
                 [com.cognitect.aws/api "0.8.391"]
                 [com.cognitect.aws/endpoints "1.1.11.664"]
                 [com.cognitect.aws/s3 "762.2.558.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [compojure "1.6.0"]
                 [honeysql "0.9.2"]
                 [kibu/pushy "0.3.8"]
                 [metosin/jsonista "0.1.1"]
                 [nilenso/honeysql-postgres "0.2.5" :exclusions [[net.cgrand/macrovich]]]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [org.clojure/core.async "1.0.567"]
                 [org.clojure/core.match "0.3.0"]
                 [org.immutant/immutant "2.1.10" :exclusions [[ring/ring-core]]]
                 [org.postgresql/postgresql "9.4-1206-jdbc41" :exclusions [[org.clojure/java.jdbc]]]
                 [ragtime "0.7.2"]
                 [reagent "0.8.1"]
                 [ring-cors "0.1.13"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-devel "1.6.3" :exclusions [[ring/ring-core]]]
                 [seancorfield/next.jdbc "1.0.5"]
                 [sudharsh/clj-oauth2 "0.5.3"]
                 [tick "0.4.23-alpha"]]
  :plugins [[lein-figwheel "0.5.19"]
            [lein-sass "0.5.0"]
            [lein-cooper "1.2.2"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
  :source-paths ["src/clj" "src/api" "src/cljc"]
  :clean-targets ["target" "dist/js" "dist/css"]
  :cljsbuild {:builds
              [{:id           "dev-ui"
                :source-paths ["src/ui" "src/cljc"]
                :figwheel     {:on-jsload "com.left-over.ui.core/mount!"}
                :compiler     {:main                 com.left-over.ui.core
                               :asset-path           "/js/compiled/out"
                               :output-to            "dist/js/compiled/app.js"
                               :output-dir           "dist/js/compiled/out"
                               :optimizations        :none
                               :source-map-timestamp true
                               :preloads             [devtools.preload]}}
               {:id           "dev-api"
                :source-paths ["src/api" "src/cljc"]
                :figwheel     true
                :compiler     {:install-deps         true
                               :npm-deps             {:cors           "2.8.5"
                                                      :express        "4.17.1"
                                                      :pg             "7.18.2"
                                                      :xmlhttprequest "1.8.0"
                                                      :ws             "7.1.2"} ;; required to use figwheel REPL
                               :main                 com.left-over.api.server
                               :asset-path           "target/js/compiled/dev"
                               :output-to            "target/js/compiled/server.js"
                               :output-dir           "target/js/compiled/dev"
                               :target               :nodejs
                               :optimizations        :none
                               :source-map-timestamp true}}
               {:id           "ui-min"
                :source-paths ["src/ui" "src/cljc"]
                :compiler     {:output-to     "dist/js/compiled/app.js"
                               :main          com.left-over.ui.core
                               :optimizations :advanced
                               :pretty-print  false}}
               {:id           "pub-images"
                :source-paths ["src/api" "src/cljc"]
                :compiler     {:install-deps  true
                               :npm-deps      {:xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.pub.images
                               :output-to     "target/pub/images.js"
                               :output-dir    "target/js/compiled/pub/images"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "pub-videos"
                :source-paths ["src/api" "src/cljc"]
                :compiler     {:install-deps  true
                               :npm-deps      {:xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.pub.videos
                               :output-to     "target/pub/videos.js"
                               :output-dir    "target/js/compiled/pub/videos"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "pub-shows"
                :source-paths ["src/api" "src/cljc"]
                :compiler     {:install-deps  true
                               :npm-deps      {:pg "7.18.2"}
                               :main          com.left-over.api.handlers.pub.shows
                               :output-to     "target/pub/shows.js"
                               :output-dir    "target/js/compiled/pub/shows"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}]}
  :aliases {"migrations" ["run" "-m" "com.left-over.api.services.db.migrations/-main"]}
  :cooper {"cljs"      ["lein" "with-profile" "dev-ui" "figwheel" "dev-ui"]
           "sass"      ["lein" "sass" "auto"]
           "server"    ["lein" "run"]
           "api-proxy" ["lein" "with-profile" "dev-api" "figwheel" "dev-api"]
           "api-repl"  ["bin/sleepnode.sh" "target/js/compiled/server.js"]}
  :sass {:src              "src/scss"
         :output-directory "dist/css/"}
  :profiles {:dev     {:dependencies  [[binaryage/devtools "0.9.10"]
                                       [cider/piggieback "0.4.0"]
                                       [figwheel-sidecar "0.5.19"]
                                       [ring/ring-core "1.3.2"]]
                       :source-paths  ["src/clj" "src/cljc" "dev"]
                       :main          com.left-over.api.server/-dev}
             :dev-ui  {:dependencies  [[binaryage/devtools "0.9.10"]
                                       [cider/piggieback "0.4.0"]
                                       [figwheel-sidecar "0.5.19"]]
                       :figwheel      {:css-dirs     ["dist/css"]
                                       :nrepl-port   7888
                                       :ring-handler com.left-over.dev-server/handler}
                       :source-paths  ["src/ui" "src/cljc" "dev"]}
             :dev-api {:dependencies  [[binaryage/devtools "0.9.10"]
                                       [cider/piggieback "0.4.0"]
                                       [figwheel-sidecar "0.5.19"]]
                       :figwheel      {:nrepl-port  7999
                                       :server-port 3559}
                       :source-paths  ["src/api" "src/cljc" "dev"]}})
