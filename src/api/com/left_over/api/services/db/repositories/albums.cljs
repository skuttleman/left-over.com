(ns com.left-over.api.services.db.repositories.albums
  (:require
    [com.left-over.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/albums
      entities/select
      (entities/with-alias :albums)
      (cond-> clause (assoc :where clause))))
