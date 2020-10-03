(ns com.left-over.api.services.db.repositories.media-links
  (:require
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.maps :as maps]))

(defmethod repos/->api ::model
  [_ media-link]
  (maps/update-maybe media-link :icon keyword))

(defn select-by [clause]
  (-> entities/media-links
      entities/select
      (entities/with-alias :media-links)
      (cond-> clause (assoc :where clause))))
