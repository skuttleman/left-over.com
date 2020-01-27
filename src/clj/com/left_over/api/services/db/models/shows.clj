(ns com.left-over.api.services.db.models.shows
  (:require
    [com.left-over.api.services.db.repositories.shows :as repo.shows]
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.models.shared :as models]
    [com.left-over.api.services.db.repositories.core :as repos]))

(defn ^:private select* [clause]
  (-> clause
      repo.shows/select-by
      (entities/inner-join entities/users :creator [:= :creator.id :shows.created-by])
      (entities/inner-join entities/locations :location [:= :location.id :shows.location-id])
      (assoc :order-by [[:shows.date-time :desc]])
      (models/select ::repo.shows/model (models/under :shows))))

(defn select-for-admin [db]
  (-> [:= :shows.deleted false]
      select*
      (repos/exec! db)))

(defn select-for-website [db]
  (-> [:and
       [:= :shows.deleted false]
       [:= :shows.hidden false]]
      select*
      (assoc-in [0 :limit] 100)
      (repos/exec! db)))
