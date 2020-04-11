(ns com.left-over.api.services.db.repositories.shows
  (:require
    [clojure.set :as set]
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.shared.utils.dates :as dates]))

(defmethod repos/->api ::model
  [_ show]
  (-> show
      (set/rename-keys {:hidden    :hidden?
                        :deleted   :deleted?
                        :confirmed :confirmed?})
      (maps/update-in-maybe [:temp-data :start :dateTime] dates/parse)
      (maps/update-in-maybe [:temp-data :end :dateTime] dates/parse)))

(defmethod repos/->db ::model
  [_ show]
  (-> show
      (set/rename-keys {:hidden?    :hidden
                        :deleted?   :deleted
                        :confirmed? :confirmed})))

(defn select-by [clause]
  (-> entities/shows
      entities/select
      (entities/with-alias :shows)
      (cond-> clause (assoc :where clause))))
