(ns com.left-over.api.server
  (:gen-class)
  (:require
    [com.left-over.api.routes.core :as routes]
    [com.left-over.api.services.env :as env]
    [immutant.web :as web]
    [nrepl.server :as nrepl]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (java.net InetAddress)))

(defn ^:private server-port [key fallback]
  (-> key
      (env/get fallback)
      str
      Integer/parseInt))

(defn ^:private run [port app]
  (let [server (web/run app {:port port :host "0.0.0.0"})]
    (println "Server is listening on port" port)
    server))

(defn ^:private start! [port app]
  (run port app))

(defn -main [& _]
  (start! (server-port :port 3000) #'routes/app))

(defn -dev [& _]
  (let [port (server-port :port 3000)
        nrepl-port (server-port :nrepl-port 7000)
        base-url (format "http://%s:%d" (.getCanonicalHostName (InetAddress/getLocalHost)) port)
        server (start! port #'routes/app-dev)]
    (alter-var-root #'env/get merge {:dev? true :base-url base-url :port port})
    (println "Server is running with #'wrap-reload at" base-url)
    (nrepl/start-server :port nrepl-port)
    (println "REPL is listening on port" nrepl-port)
    server))

