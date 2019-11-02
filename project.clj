(defproject com.left-over/website "0.1.0-SNAPSHOT"
  :description "Left Over website"
  :url "https://www.github.com/skuttleman/left-over.com"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main com.left-over.api.server
  :aot [com.left-over.api.server]
  :min-lein-version "2.9.1"
  :dependencies [[bidi "2.1.3" :exclusions [[ring/ring-core]]]
                 [clj-http "3.9.1"]
                 [cljs-http "0.1.46"]
                 [com.ben-allred/collaj "0.8.0"]
                 [com.ben-allred/vow "0.1.0"]
                 [com.cognitect.aws/api "0.8.391"]
                 [com.cognitect.aws/endpoints "1.1.11.664"]
                 [com.cognitect.aws/s3 "762.2.558.0"]
                 [compojure "1.6.0"]
                 [environ "1.1.0"]
                 [kibu/pushy "0.3.8"]
                 [metosin/jsonista "0.1.1"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.500"]
                 [org.immutant/immutant "2.1.10" :exclusions [[ring/ring-core]]]
                 [reagent "0.8.1"]
                 [ring-cors "0.1.13"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-devel "1.6.3" :exclusions [[ring/ring-core]]]]
  :plugins [[lein-figwheel "0.5.19"]
            [lein-sass "0.5.0"]
            [lein-cooper "1.2.2"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs" "src/cljc" "dev"]
                :figwheel     {:on-jsload "com.left-over.ui.core/mount!"}
                :compiler     {:main                 com.left-over.ui.core
                               :asset-path           "js/compiled/out"
                               :output-to            "dist/js/compiled/app.js"
                               :output-dir           "dist/js/compiled/out"
                               :source-map-timestamp true
                               :preloads             [devtools.preload]}}
               {:id           "min"
                :source-paths ["src/cljs" "src/cljc"]
                :compiler     {:output-to     "dist/js/compiled/app.js"
                               :main          com.left-over.ui.core
                               :optimizations :advanced
                               :pretty-print  false}}]}
  :cooper {"cljs" ["lein" "figwheel"]
           "sass" ["lein" "sass" "auto"]
           "server" ["lein" "run"]}
  :sass {:src              "src/scss"
         :output-directory "dist/css/"}
  :figwheel {:css-dirs     ["dist/css"]
             :nrepl-port   7888
             :ring-handler com.left-over.dev-server/handler}
  :profiles {:dev {:dependencies  [[binaryage/devtools "0.9.10"]
                                   [cider/piggieback "0.4.0"]
                                   [figwheel-sidecar "0.5.19"]
                                   [ring/ring-core "1.3.2"]]
                   :source-paths  ["src/clj" "src/cljs" "src/cljc" "dev"]
                   :main          com.left-over.api.server/-dev
                   :clean-targets ^{:protect false} ["dist/js/compiled"
                                                     "dist/css"
                                                     :target-path]}})
