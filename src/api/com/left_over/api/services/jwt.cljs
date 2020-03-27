(ns com.left-over.api.services.jwt
  (:require
    [com.left-over.common.services.env :as env]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.numbers :as numbers]
    jwt-simple))

(defn now []
  (.getTime (js/Date.)))

(defn in-days [x]
  (* 1000 60 60 24 x))

(defn decode [token]
  (try
    (-> token
        (jwt-simple/decode (env/get :jwt-secret))
        (js->clj :keywordize-keys true)
        (update :data edn/parse))
    (catch :default _ nil)))

(defn encode [payload]
  (let [days-to-expire (numbers/parse-int (env/get :jwt-expiration "30"))
        now (now)]
    (-> {:iat  (-> now
                   (/ 1000)
                   js/Math.floor)
         :data (edn/stringify payload)
         :exp  (-> now
                   (+ (in-days days-to-expire))
                   (/ 1000)
                   js/Math.floor)}
        (cond-> (:id payload) (assoc :sub (str (:id payload))))
        clj->js
        (jwt-simple/encode (env/get :jwt-secret)))))
