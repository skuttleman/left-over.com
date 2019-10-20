(defproject com.left-over/website "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.9.1"
  :dependencies [[bidi "2.1.3" :exclusions [[ring/ring-core]]]
                 [com.ben-allred/collaj "0.8.0"]
                 [kibu/pushy "0.3.8"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.500"]
                 [reagent "0.8.1"]]
  :plugins [[lein-figwheel "0.5.19"]
            [lein-sass "0.5.0"]
            [lein-cooper "1.2.2"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
  :source-paths ["src/cljs"]
  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs" "dev"]
                :figwheel     {:on-jsload "com.left-over.ui.core/mount!"}
                :compiler     {:main                 com.left-over.ui.core
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/app.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map-timestamp true
                               :preloads             [devtools.preload]}}
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/app.js"
                               :main          com.left-over.ui.core
                               :optimizations :advanced
                               :pretty-print  false}}]}
  :cooper {"cljs" ["lein" "figwheel"]
           "sass" ["lein" "sass" "auto"]}
  :sass {:src              "src/scss"
         :output-directory "resources/public/css/"}
  :figwheel {:css-dirs     ["resources/public/css"]
             :nrepl-port   7888
             :ring-handler com.left-over.server/handler}
  :profiles {:dev {:dependencies  [[binaryage/devtools "0.9.10"]
                                   [cider/piggieback "0.4.0"]
                                   [figwheel-sidecar "0.5.19"]
                                   [ring/ring-core "1.3.2"]]
                   :source-paths  ["src/cljs" "dev"]
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     "resources/public/css"
                                                     :target-path]}})
