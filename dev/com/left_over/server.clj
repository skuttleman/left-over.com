(ns com.left-over.server
  (:require [clojure.string :refer [starts-with?]]
            [ring.middleware.resource :refer [wrap-resource]]))

(defn- wrap-default-index [app]
  (fn [request]
    (app
      (cond-> request
        (not (re-matches #"/(css|js|images)/.*" (:uri request))) (assoc :uri "/index.html")))))

(def handler
  (-> (fn [_] {:status 404 :body "static asset not found"})
      (wrap-resource "public")
      wrap-default-index))
