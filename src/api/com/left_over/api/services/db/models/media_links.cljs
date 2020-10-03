(ns com.left-over.api.services.db.models.media-links
  (:require [com.left-over.api.services.db.repositories.media-links :as repo.media-links]
            [com.left-over.api.services.db.models.core :as models]
            [com.left-over.api.services.db.repositories.core :as repos]))

(defn ^:private select* [conn clause]
  (-> clause
      repo.media-links/select-by
      (models/select ::repo.media-links/model (models/under :media-links))
      (repos/exec! conn)))

(defn select-by-album-ids [conn album-ids]
  (select* conn [:in :media-links.album-id album-ids]))
