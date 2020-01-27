(ns com.left-over.api.services.db.repositories.shows
  (:require
    [clojure.set :as set]
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.preparations :as prep]
    [com.left-over.api.services.db.repositories.core :as repos]))

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

(defn select-by [clause]
  (-> entities/shows
      entities/select
      (entities/with-alias :shows)
      (cond-> clause (assoc :where clause))))
