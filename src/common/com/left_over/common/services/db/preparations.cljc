(ns com.left-over.common.services.db.preparations
  (:require
    [camel-snake-kebab.core :as csk]
    [com.left-over.shared.utils.keywords :as keywords])
  #?(:clj (:import
            (java.sql Timestamp)
            (org.postgresql.util PGobject))))

#?(:clj
   (defn ^:private for-type [type]
     (let [type (csk/->snake_case_string type)]
       (fn [value]
         (doto (PGobject.)
           (.setType type)
           (.setValue (keywords/safe-name value)))))))

(defn prepare [->sql-value table]
  (partial into
           {}
           (map (fn [[k v]]
                  [(csk/->snake_case_keyword k) (->sql-value table k v)]))))

(def timestamp
  #?(:clj  (comp (for-type :timestamp) #(str (Timestamp. (.getTime %))))
     :cljs identity))
