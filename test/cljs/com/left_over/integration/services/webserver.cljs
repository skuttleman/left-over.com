(ns com.left-over.integration.services.webserver
  (:require
    [clojure.string :as string]
    [com.ben-allred.espresso.core :as es]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.env :as env]
    [com.left-over.api.services.fs :as fs]
    [com.left-over.api.server :as server]))

(def HOME "http://localhost:3449")

(def ADMIN "http://localhost:3449/admin")

(defn API [path]
  (str (env/get :aws-api-uri) path))

(defn ^:private handler [{:keys [path]}]
  (-> (fs/read-file (str "dist" path))
      (v/catch (fn [_]
                 (cond
                   (= path "/privacy") (fs/read-file "resources/privacy.html")
                   (string/starts-with? path "/admin") (fs/read-file "dist/admin.html")
                   :else (fs/read-file "dist/index.html"))))
      (v/then (fn [body]
                {:status 200 :body body}))))

(def server
  (-> handler
      es/create-server
      (server/->WebServer "StaticServer" 3449)))
