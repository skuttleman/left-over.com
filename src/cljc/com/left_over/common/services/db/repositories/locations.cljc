(ns com.left-over.common.services.db.repositories.locations
  (:require
    [com.left-over.common.services.db.entities :as entities]
    [com.left-over.common.services.db.preparations :as prep]
    [com.left-over.common.services.db.repositories.core :as repos]))

(defmethod repos/->sql-value [:locations :created-at]
  [_ _ value]
  (prep/timestamp value))

(defmethod repos/->sql-value [:locations :updated-at]
  [_ _ value]
  (prep/timestamp value))

(defn select-by [clause]
  (-> entities/locations
      entities/select
      (entities/with-alias :locations)
      (cond-> clause (assoc :where clause))))
