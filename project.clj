(defproject com.left-over/website "0.1.0-SNAPSHOT"
  :description "Left Over website"
  :url "https://www.github.com/skuttleman/left-over.com"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.9.1"
  :dependencies [[bidi "2.1.3" :exclusions [[ring/ring-core]]]
                 [camel-snake-kebab "0.4.1"]
                 [cljs-http "0.1.46"]
                 [cljsjs/moment "2.24.0-0"]
                 [cljsjs/moment-timezone "0.5.11-1"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.ben-allred/collaj "0.8.0"]
                 [com.ben-allred/formation "0.6.2"]
                 [com.ben-allred/vow "0.6.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [honeysql "0.9.2"]
                 [kibu/pushy "0.3.8"]
                 [lambdaisland/uri "1.2.1"]
                 [nilenso/honeysql-postgres "0.2.5" :exclusions [[net.cgrand/macrovich]]]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [org.clojure/core.async "1.1.587"]
                 [org.clojure/core.match "0.3.0"]
                 [reagent "0.8.1"]]
  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-cooper "1.2.2"]
            [lein-doo "0.1.10"]
            [lein-figwheel "0.5.19"]
            [lein-sass "0.5.0"]]
  :source-paths ["src/shared"]
  :test-paths ["test/cljs"]
  :clean-targets ["target" "dist/js" "dist/css" "node_modules" "package.json" "package-lock.json"]
  :figwheel {:load-all-builds false}
  :cljsbuild {:builds
              [{:id           "dev-ui"
                :source-paths ["src/ui" "src/shared"]
                :figwheel     {:on-jsload "com.left-over.ui.core/mount!"}
                :compiler     {:install-deps         true
                               :npm-deps             {:jwt-simple     "0.5.6"
                                                      :pg             "8.1.0"
                                                      :xmlhttprequest "1.8.0"
                                                      :ws             "7.1.2"} ;; required to use figwheel REPL
                               :main                 com.left-over.ui.core
                               :asset-path           "/js/compiled/out"
                               :output-to            "dist/js/compiled/app.js"
                               :output-dir           "dist/js/compiled/out"
                               :optimizations        :none
                               :source-map-timestamp true
                               :preloads             [devtools.preload]}}
               {:id           "dev-api"
                :source-paths ["src/api" "src/shared"]
                :figwheel     {:on-jsload "com.left-over.api.server/restart!"}
                :compiler     {:install-deps         true
                               :npm-deps             {:jwt-simple     "0.5.6"
                                                      :pg             "8.1.0"
                                                      :xmlhttprequest "1.8.0"
                                                      :ws             "7.1.2"} ;; required to use figwheel REPL
                               :main                 com.left-over.api.server
                               :asset-path           "target/js/compiled/dev"
                               :output-to            "target/js/compiled/server.js"
                               :output-dir           "target/js/compiled/dev"
                               :target               :nodejs
                               :optimizations        :none
                               :source-map-timestamp true}}
               {:id           "ui-main"
                :source-paths ["src/ui"]
                :compiler     {:output-to     "dist/js/compiled/main.js"
                               :output-dir    "dist/js/compiled/main"
                               :main          com.left-over.ui.main
                               :optimizations :advanced
                               :pretty-print  false}}
               {:id           "ui-admin"
                :source-paths ["src/ui"]
                :compiler     {:output-to     "dist/js/compiled/admin.js"
                               :output-dir    "dist/js/compiled/admin"
                               :main          com.left-over.ui.admin
                               :optimizations :advanced
                               :pretty-print  false}}
               {:id           "pub-images"
                :source-paths ["src/api"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple     "0.5.6"
                                               :pg             "8.1.0"
                                               :xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.pub.images
                               :output-to     "target/pub/images.js"
                               :output-dir    "target/js/compiled/pub/images"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "pub-videos"
                :source-paths ["src/api"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple     "0.5.6"
                                               :pg             "8.1.0"
                                               :xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.pub.videos
                               :output-to     "target/pub/videos.js"
                               :output-dir    "target/js/compiled/pub/videos"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "pub-shows"
                :source-paths ["src/api"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple     "0.5.6"
                                               :pg             "8.1.0"
                                               :xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.pub.shows
                               :output-to     "target/pub/shows.js"
                               :output-dir    "target/js/compiled/pub/shows"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "admin-locations"
                :source-paths ["src/api"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple     "0.5.6"
                                               :pg             "8.1.0"
                                               :xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.admin.locations
                               :output-to     "target/admin/locations.js"
                               :output-dir    "target/js/compiled/admin/locations"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "admin-shows"
                :source-paths ["src/api"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple     "0.5.6"
                                               :pg             "8.1.0"
                                               :xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.admin.shows
                               :output-to     "target/admin/shows.js"
                               :output-dir    "target/js/compiled/admin/shows"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "auth"
                :source-paths ["src/api"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple     "0.5.6"
                                               :pg             "8.1.0"
                                               :xmlhttprequest "1.8.0"}
                               :main          com.left-over.api.handlers.auth
                               :output-to     "target/auth.js"
                               :output-dir    "target/js/compiled/auth"
                               :target        :nodejs
                               :optimizations :simple
                               :pretty-print  true}}
               {:id           "test"
                :source-paths ["src/api" "src/ui" "test/cljs"]
                :compiler     {:install-deps  true
                               :npm-deps      {:jwt-simple         "0.5.6"
                                               :pg                 "8.1.0"
                                               :xmlhttprequest     "1.8.0"
                                               :selenium-webdriver "4.0.0-alpha.7"}
                               :output-dir    "target/js/compiled/tests"
                               :output-to     "target/js/tests.js"
                               :target        :nodejs
                               :main          com.left-over.test.runner
                               :optimizations :none}}]}
  :doo {:build "test"
        :alias {:default [:node]}}
  :aliases {"migrations" ["run" "-m" "com.left-over.api.services.db.migrations/-main"]}
  :cooper {"api"  ["bin/with-profile.sh" "api" "figwheel" "dev-api"]
           "api " ["bin/sleepnode.sh" "target/js/compiled/server.js"]
           "ui"   ["bin/with-profile.sh" "ui" "figwheel" "dev-ui"]
           "sass" ["lein" "sass" "auto"]}
  :sass {:src              "src/scss"
         :output-directory "dist/css/"}
  :profiles {:ui   {:dependencies [[binaryage/devtools "0.9.10"]
                                   [cider/piggieback "0.4.0"]
                                   [figwheel-sidecar "0.5.19"]]
                    :figwheel     {:css-dirs     ["dist/css"]
                                   :nrepl-port   7888
                                   :ring-handler com.left-over.dev-server/handler}
                    :source-paths ["src/ui" "dev"]}
             :api  {:dependencies [[binaryage/devtools "0.9.10"]
                                   [cider/piggieback "0.4.0"]
                                   [com.ben-allred/espresso "0.2.0"]
                                   [figwheel-sidecar "0.5.19"]]
                    :figwheel     {:nrepl-port  7999
                                   :server-port 3559}
                    :source-paths ["src/api" "dev"]}
             :test {:dependencies [[binaryage/devtools "0.9.10"]
                                   [cider/piggieback "0.4.0"]
                                   [cljstache "2.0.5"]
                                   [com.ben-allred/espresso "0.2.0"]
                                   [figwheel-sidecar "0.5.19"]]
                    :source-paths ["src/api" "src/ui" "dev"]}})
