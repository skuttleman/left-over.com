(ns com.left-over.api.services.db.models.users
  (:require
    [com.left-over.api.services.db.repositories.users :as repo.users]
    [com.left-over.api.services.db.models.shared :as models]
    [com.left-over.api.services.db.repositories.core :as repos]))

(defn ^:private select* [db clause]
  (-> clause
      repo.users/select-by
      (models/select ::repo.users/model (models/under :users))
      (repos/exec! db)))

(defn select-all [db]
  (select* db nil))
