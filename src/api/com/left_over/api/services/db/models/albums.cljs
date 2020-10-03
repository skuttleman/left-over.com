(ns com.left-over.api.services.db.models.albums
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.db.models.core :as models]
    [com.left-over.api.services.db.models.media-links :as media-links]
    [com.left-over.api.services.db.models.songs :as songs]
    [com.left-over.api.services.db.repositories.albums :as repo.albums]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.db.utils :as db.utils]))

(defn ^:private select* [conn clause]
  (-> clause
      repo.albums/select-by
      (models/select ::repo.albums/model (models/under :albums))
      (repos/exec! conn)
      (v/then-> (db.utils/assoc-coll-maps conn
                                          media-links/select-by-album-ids
                                          :media-links
                                          [:id :album-id])
                (db.utils/assoc-coll-maps conn
                                          songs/select-by-album-ids
                                          :songs
                                          [:id :album-id]))))

(defn select-all [conn]
  (select* conn [:= 1 1]))
