(ns com.left-over.dev-server
  (:require [clojure.string :refer [starts-with?]]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.java.io :as io]))

(defn handler [request]
  (let [file (io/file (str "dist" (:uri request)))]
    (if (.exists file)
      {:status 200 :body file}
      {:status 200 :body (io/file "dist/index.html")})))
