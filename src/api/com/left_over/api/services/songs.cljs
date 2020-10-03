(ns com.left-over.api.services.songs
  (:require
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.memoize :as memo]
    [com.left-over.api.services.db.models.albums :as albums]
    [com.left-over.shared.utils.numbers :as numbers]
    [com.left-over.api.services.env :as env]))

(defn ^:private songs* [_event]
  (repos/transact albums/select-all))

(def ^{:arglists '([event])} songs
  (memo/memo songs*
             (numbers/parse-int (env/get :db-cache-ttl))
             (numbers/parse-int (env/get :db-max-cache-ttl))))
