(ns com.left-over.dev-server
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]))

(defn ^:private dev-serve [file]
  (string/replace (slurp (io/file file)) #"[a-z]+\.js" "app.js"))

(defn handler [{:keys [uri]}]
  (let [file (io/file (str "dist" uri))]
    (cond
      (.isFile file) {:status 200 :body file}
      (= uri "/privacy") {:status 200 :body (io/input-stream (io/resource "privacy.html"))}
      (string/starts-with? uri "/admin") {:status 200 :body (dev-serve "dist/admin.html")}
      :else {:status 200 :body (dev-serve "dist/index.html")})))
