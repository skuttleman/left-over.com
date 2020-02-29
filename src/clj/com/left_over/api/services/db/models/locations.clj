(ns com.left-over.api.services.db.models.locations
  (:require
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.models.shared :as models]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.db.repositories.locations :as repo.locations]
    [com.left-over.api.services.db.repositories.shows :as repo.shows]
    [com.left-over.common.utils.colls :as colls])
  (:import (java.util Date)))

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

(defn find-by-id [db location-id]
  (colls/only! (select* db [:= :locations.id location-id])))

(defn save [db user {location-id :id :as location}]
  (let [location' (-> location
                      (dissoc :id)
                      (assoc :updated-at (Date.))
                      (cond->
                        (not location-id) (assoc :created-by (:id user) :created-at (Date.))))]
    (-> entities/locations
        (cond->
          location-id (-> (entities/modify location' [:= :locations.id location-id])
                          (models/modify entities/locations ::repo.locations/model))
          (not location-id) (-> (entities/insert-into [location'])
                                (models/insert-many entities/locations ::repo.locations/model)))

        (repos/exec! db)
        first
        :id
        (->> (find-by-id db)))))
