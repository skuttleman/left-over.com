(ns com.left-over.dev-server
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]))

(defn handler [{:keys [uri]}]
  (let [file (io/file (str "dist" uri))]
    (cond
      (.isFile file) {:status 200 :body file}
      (string/starts-with? uri "/admin") {:status 200 :body (io/file "dist/admin.html")}
      :else {:status 200 :body (io/file "dist/index.html")})))
