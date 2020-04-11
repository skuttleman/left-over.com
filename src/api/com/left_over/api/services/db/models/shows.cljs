(ns com.left-over.api.services.db.models.shows
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.models.core :as models]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.db.repositories.shows :as repo.shows]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.maps :as maps]))

(defn ^:private select* [clause]
  (-> clause
      repo.shows/select-by
      (entities/inner-join (update entities/users :fields disj :token-info)
                           :creator
                           [:= :creator.id :shows.created-by])
      (entities/left-join entities/locations
                          :location
                          [:= :location.id :shows.location-id])
      (assoc :order-by [[:shows.date-time :desc]])
      (models/select ::repo.shows/model (models/under :shows))))

(defn select-for-admin [db]
  (-> [:and
       [:= :shows.deleted false]
       [:= :shows.confirmed true]]
      select*
      (repos/exec! db)))

(defn select-for-website [db]
  (-> [:and
       [:= :shows.deleted false]
       [:= :shows.confirmed true]
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

(defn select-new-event-ids [db event-ids]
  (-> {:select [[:shows.event-id "shows/event-id"]]
       :from   [:shows]
       :where  [:in :shows.event-id event-ids]}
      (cons [nil (map :event-id)])
      (repos/exec! db)
      (v/then-> set (remove event-ids))))

(defn select-unmerged [db]
  (-> [:and
       [:= :shows.deleted false]
       [:= :shows.confirmed false]]
      select*
      (repos/exec! db)
      (v/then-> (->> (sort-by (comp (some-fn #(some-> % :dateTime dates/stringify) :date)
                                    :start
                                    :temp-data))))))

(defn save [db user {show-id :id :as show}]
  (let [show' (-> show
                  (dissoc :id)
                  (assoc :updated-at (dates/now))
                  (maps/default :timezone "America/New_York")
                  (cond->
                    (not show-id) (assoc :created-by (:id user)
                                         :created-at (dates/now))
                    (not (:deleted? show)) (assoc :confirmed? true)))]
    (-> entities/shows
        (cond->
          show-id (-> (entities/modify show' [:= :shows.id show-id])
                      (models/modify entities/shows ::repo.shows/model))
          (not show-id) (-> (entities/insert-into [show'])
                            (models/insert-many entities/shows ::repo.shows/model)))
        (repos/exec! db)
        (v/then-> first :id (->> (find-by-id db))))))

(defn delete [db show-ids]
  (-> entities/shows
      (entities/modify {:deleted?   true
                        :updated-at (dates/now)}
                       [:in :shows.id show-ids])
      (models/modify entities/shows ::repo.shows/model)
      (repos/exec! db)))

(defn save-temp-data [db user shows]
  (-> shows
      (->> (map (fn [{event-id :id :as show}]
                  {:event-id   event-id
                   :created-by (:id user)
                   :updated-at (dates/now)
                   :temp-data  (clj->js (dissoc show :id))
                   :hidden?    true}))
           (entities/insert-into entities/shows))
      (models/insert-many entities/shows ::repo.shows/model)
      (repos/exec! db)))

(defn refresh-events [conn user shows]
  (v/await [[new-shows old-shows] (some-> shows
                                          seq
                                          (->> (map :id) (select-new-event-ids conn))
                                          (v/then-> set (comp :id) (colls/organize shows)))]
    (v/all [(v/vow (some->> new-shows seq (save-temp-data conn user)))
            (v/resolve old-shows)])))
