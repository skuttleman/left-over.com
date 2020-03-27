(ns com.left-over.common.services.db.repositories.users
  (:require
    [com.left-over.common.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/users
      entities/select
      (entities/with-alias :users)
      (cond-> clause (assoc :where clause))))

(defn merge-token-info [token-info clause]
  (-> entities/users
      (entities/modify clause)
      (entities/json-merge :token-info token-info)))
