(ns com.left-over.api.services.jwt
  (:require
    [com.left-over.api.services.env :as env]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.numbers :as numbers]
    jwt-simple))

(defn in-days [x]
  (* 60 60 24 x))

(defn decode [token]
  (try
    (-> token
        (jwt-simple/decode (env/get :jwt-secret))
        (js->clj :keywordize-keys true)
        (update :data edn/parse))
    (catch :default _ nil)))

(defn encode [payload]
  (let [days-to-expire (numbers/parse-int (env/get :jwt-expiration "30"))
        iat (-> (dates/now)
                dates/inst->ms
                (/ 1000)
                js/Math.floor)]
    (-> {:iat  iat
         :data (edn/stringify payload)
         :exp  (+ iat (in-days days-to-expire))}
        (cond-> (:id payload) (assoc :sub (str (:id payload))))
        clj->js
        (jwt-simple/encode (env/get :jwt-secret)))))
