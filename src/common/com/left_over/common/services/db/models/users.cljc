(ns com.left-over.common.services.db.models.users
  (:require
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [com.left-over.common.services.db.repositories.users :as repo.users]
    [com.left-over.common.services.db.models.core :as models]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.colls :as colls]))

(defn ^:private select* [db clause]
  (-> clause
      repo.users/select-by
      (models/select ::repo.users/model (models/under :users))
      (repos/exec! db)))

(defn find-by-email [db email]
  (v/then (select* db [:= :users.email email]) colls/only!))

(defn merge-token-info [db user-id token-info]
  (-> token-info
      (repo.users/merge-token-info [:= :users.id user-id])
      (repos/exec! db)
      (v/then colls/only!)))
