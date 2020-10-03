(ns com.left-over.api.services.db.models.songs
  (:require
    [com.left-over.api.services.db.models.core :as models]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.db.repositories.songs :as repo.songs]
    [com.left-over.api.services.db.entities :as entities]))

(defn ^:private select* [conn clause]
  (-> clause
      repo.songs/select-by
      (entities/order :tracknum :asc)
      (models/select ::repo.songs/model (models/under :songs))
      (repos/exec! conn)))

(defn select-by-album-ids [conn album-ids]
  (select* conn [:and
                 [:= :songs.visible true]
                 [:in :songs.album-id album-ids]]))
