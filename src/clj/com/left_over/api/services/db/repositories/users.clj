(ns com.left-over.api.services.db.repositories.users
  (:require [com.left-over.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/users
      entities/select
      (entities/with-alias :users)
      (cond-> clause (assoc :where clause))))
