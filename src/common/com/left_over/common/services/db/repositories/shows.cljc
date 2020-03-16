(ns com.left-over.common.services.db.repositories.shows
  (:require
    [clojure.set :as set]
    [com.left-over.common.services.db.entities :as entities]
    [com.left-over.common.services.db.preparations :as prep]
    [com.left-over.common.services.db.repositories.core :as repos]))

(defmethod repos/->api ::model
  [_ hangout]
  (-> hangout
      (set/rename-keys {:hidden  :hidden?
                        :deleted :deleted?})))

(defmethod repos/->db ::model
  [_ hangout]
  (-> hangout
      (set/rename-keys {:hidden?  :hidden
                        :deleted? :deleted})))

(defmethod repos/->sql-value [:shows :date-time]
  [_ _ value]
  (prep/timestamp value))

(defmethod repos/->sql-value [:shows :created-at]
  [_ _ value]
  (prep/timestamp value))

(defmethod repos/->sql-value [:shows :updated-at]
  [_ _ value]
  (prep/timestamp value))

(defn select-by [clause]
  (-> entities/shows
      entities/select
      (entities/with-alias :shows)
      (cond-> clause (assoc :where clause))))
