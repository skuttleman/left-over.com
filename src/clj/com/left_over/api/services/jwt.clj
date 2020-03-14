(ns com.left-over.api.services.jwt
  (:require
    [clj-jwt.core :as clj-jwt]
    [com.left-over.api.services.env :as env]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.dates :as dates])
  (:import (org.joda.time DateTime)))

(defn ^:private decode* [token]
  (try
    (clj-jwt/str->jwt token)
    (catch Throwable _
      nil)))

(defn decode [value]
  (some-> value
          decode*
          :claims
          (update :data edn/parse)))

(defn encode [payload]
  (let [days-to-expire (env/get :jwt-expiration 30)
        jwt-secret (env/get :jwt-secret)
        now (dates/now)]
    (-> {:iat  (-> now
                   dates/inst->ms
                   DateTime.)
         :data (edn/stringify payload)
         :exp  (-> now
                   (dates/plus days-to-expire :days)
                   dates/inst->ms
                   DateTime.)}
        clj-jwt/jwt
        (clj-jwt/sign :HS256 jwt-secret)
        clj-jwt/to-str)))
