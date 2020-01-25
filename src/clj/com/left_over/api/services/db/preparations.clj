(ns com.left-over.api.services.db.preparations
  (:refer-clojure :exclude [time])
  (:require
    [camel-snake-kebab.core :as csk]
    [com.left-over.common.utils.keywords :as keywords])
  (:import
    (java.sql Timestamp)
    (org.postgresql.util PGobject)))

(defn ^:private for-type [type]
  (let [type (csk/->snake_case_string type)]
    (fn [value]
      (doto (PGobject.)
        (.setType type)
        (.setValue (keywords/safe-name value))))))

(defn prepare [->sql-value table]
  (partial into
           {}
           (map (fn [[k v]]
                  [(csk/->snake_case_keyword k) (->sql-value table k v)]))))

(def timestamp (comp (for-type :timestamp) #(str (Timestamp. (.getTime %)))))
