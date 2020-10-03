(ns com.left-over.api.services.db.repositories.songs
  (:require
    [clojure.set :as set]
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.repositories.core :as repos]))

(defmethod repos/->api ::model
  [_ show]
  (set/rename-keys show {:visible :visible?}))

(defmethod repos/->db ::model
  [_ show]
  (set/rename-keys show {:visible? :visible}))

(defn select-by [clause]
  (-> entities/songs
      entities/select
      (entities/with-alias :songs)
      (cond-> clause (assoc :where clause))))
