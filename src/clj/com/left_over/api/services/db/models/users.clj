(ns com.left-over.api.services.db.models.users
  (:require
    [com.left-over.api.services.db.models.shared :as models]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.db.repositories.users :as repo.users]))

(defn ^:private select* [db clause]
  (-> clause
      repo.users/select-by
      (models/select ::repo.users/model (models/under :users))
      (repos/exec! db)))

(defn find-by-email [db email]
  (first (select* db [:= :users.email email])))
