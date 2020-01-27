(ns com.left-over.api.services.middleware
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [com.left-over.api.services.db.repositories.core :as repos]
            [com.left-over.api.services.jwt :as jwt]
            [com.left-over.common.utils.edn :as edn]
            [com.left-over.common.utils.maps :as maps])
  (:import (java.io PushbackReader)))

(defn with-transaction [handler]
  (fn [request]
    (repos/transact
      (fn [db]
        (handler (assoc request :db db))))))

(defn with-content-type [handler]
  (fn [request]
    (let [content-type "application/edn"
          [stringify parse] (case content-type
                              "application/edn" [edn/stringify edn/parse]
                              [identity identity])]
      (let [{:keys [headers] :as response} (-> request
                                               (maps/update-maybe :body (comp parse #(PushbackReader. (io/reader %))))
                                               handler)]
        (cond-> response
          (not (get headers "content-type")) (-> (maps/update-maybe :body stringify)
                                                 (assoc-in [:headers "content-type"] content-type)))))))

(defn with-jwt [handler]
  (fn [{:keys [params uri] :as request}]
    (let [user (when (re-find #"^(/api|/auth)" uri)
                 (some-> request
                         (get-in [:headers "authorization"] (:auth-token params))
                         (string/replace #"^Bearer " "")
                         jwt/decode
                         :data))]
      (-> request
          (maps/assoc-maybe :auth/user user)
          handler))))

(defn with-ex-handling [handler]
  (fn [request]
    (try (handler request)
      (catch Throwable ex
        (if-let [response (:response (ex-data ex))]
          response
          (throw ex))))))
