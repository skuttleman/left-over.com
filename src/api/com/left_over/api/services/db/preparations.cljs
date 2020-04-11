(ns com.left-over.api.services.db.preparations
  (:require
    [camel-snake-kebab.core :as csk]))

(defn prepare [->sql-value table]
  (partial into
           {}
           (map (fn [[k v]]
                  [(csk/->snake_case_keyword k) (->sql-value table k v)]))))
