(ns com.left-over.common.services.db.models.shows
  (:require
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [com.left-over.common.services.db.entities :as entities]
    [com.left-over.common.services.db.models.core :as models]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.services.db.repositories.shows :as repo.shows]
    [com.left-over.shared.utils.colls :as colls])
  #?(:clj (:import
            (java.util Date))))

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

(defn find-by-id [db show-id]
  (-> [:and
       [:= :shows.id show-id]
       [:= :shows.deleted false]]
      select*
      (repos/exec! db)
      (v/then colls/only!)))

(defn save [db user {show-id :id :as show}]
  (let [show' (-> show
                  (dissoc :id)
                  (assoc :updated-at #?(:clj (Date.) :cljs (js/Date.)))
                  (cond->
                    (not show-id) (assoc :created-by (:id user)
                                         :created-at #?(:clj (Date.) :cljs (js/Date.)))))]
    (-> entities/shows
        (cond->
          show-id (-> (entities/modify show' [:= :shows.id show-id])
                      (models/modify entities/shows ::repo.shows/model))
          (not show-id) (-> (entities/insert-into [show'])
                            (models/insert-many entities/shows ::repo.shows/model)))

        (repos/exec! db)
        (v/then-> first :id (->> (find-by-id db))))))
