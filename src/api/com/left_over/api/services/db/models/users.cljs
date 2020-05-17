(ns com.left-over.api.services.db.models.users
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.db.repositories.users :as repo.users]
    [com.left-over.api.services.db.models.core :as models]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.dates :as dates]))

(defn ^:private select* [conn clause]
  (-> clause
      repo.users/select-by
      (models/select ::repo.users/model (models/under :users))
      (repos/exec! conn)))

(defn find-by-email [conn email]
  (v/then (select* conn [:= :users.email email]) colls/only!))

(defn find-by-id [conn user-id]
  (v/then (select* conn [:= :users.id user-id]) colls/only!))

(defn merge-token-info [conn user-id {:keys [expires_in] :as token-info}]
  (-> token-info
      (cond->
        expires_in (assoc :expires_at (-> (dates/now)
                                          (dates/plus expires_in :seconds)
                                          dates/stringify)))
      (repo.users/merge-token-info [:= :users.id user-id])
      (update :set assoc :updated-at (dates/now))
      (repos/exec! conn)
      (v/then colls/only!)))
