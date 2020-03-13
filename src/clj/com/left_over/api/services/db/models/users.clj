(ns com.left-over.api.services.db.models.users
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.db.repositories.users :as repo.users]
    [com.left-over.common.services.db.models.shared :as models]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.utils.colls :as colls]))

(defn ^:private select* [db clause]
  (-> clause
      repo.users/select-by
      (models/select ::repo.users/model (models/under :users))
      (repos/exec! db)))

(defn find-by-email [db email]
  (v/then (select* db [:= :users.email email]) colls/only!))
