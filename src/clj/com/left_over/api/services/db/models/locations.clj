(ns com.left-over.api.services.db.models.locations
  (:require
    [com.left-over.api.services.db.models.shared :as models]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.db.repositories.locations :as repo.locations]
    [com.left-over.api.services.db.repositories.shows :as repo.shows]))

(defn ^:private select* [db clause]
  (-> clause
      repo.locations/select-by
      (assoc :order-by [[:locations.created-at :desc]
                        [(-> [:= :locations.id :shows.location-id]
                             repo.shows/select-by
                             (assoc :order-by [[:shows.updated-at :desc]]
                                    :limit 1
                                    :select [:shows.created-at]))
                         :desc]
                        [:locations.updated-at :desc]])
      (models/select ::repo.locations/model (models/under :locations))
      (repos/exec! db)))

(defn select-for-admin [db]
  (select* db nil))
