(ns com.left-over.api.services.db.repositories.locations
  (:require
    [com.left-over.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/locations
      entities/select
      (entities/with-alias :locations)
      (cond-> clause (assoc :where clause))))
