(ns com.left-over.api.services.middleware
  (:require [com.left-over.api.services.db.repositories.core :as repos]
            [com.left-over.common.utils.edn :as edn]
            [com.left-over.common.utils.maps :as maps]
            [clojure.java.io :as io])
  (:import (java.io PushbackReader)))

(defn with-transaction [handler]
  (fn [request]
    (repos/transact
      (fn [db]
        (handler (assoc request :db db))))))

(defn with-content-type [handler]
  (fn [request]
    (let [content-type (get-in request [:headers "accept"] "application/edn")
          content-type (if (#{"*" "*/*"} content-type) "application/edn" content-type)
          [stringify parse] (case content-type
                              [edn/stringify edn/parse])]
      (let [{:keys [headers] :as response} (-> request
                                               (maps/update-maybe :body (comp parse #(PushbackReader. (io/reader %))))
                                               handler)]
        (cond-> response
          (not (get headers "content-type")) (-> (maps/update-maybe :body stringify)
                                                 (assoc-in [:headers "content-type"] content-type)))))))
