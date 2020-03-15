(ns com.left-over.api.services.shows
  (:require
    [com.left-over.common.services.db.models.shows :as shows]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.api.services.env :as env]
    [com.left-over.common.utils.memoize :as memo]
    [com.left-over.common.utils.numbers :as numbers]))

(defn ^:private shows* [_event]
  (repos/transact shows/select-for-website))

(def ^{:arglists '([event])} shows
  (memo/memo shows*
             (numbers/parse-int (env/get :db-cache-ttl))
             (numbers/parse-int (env/get :db-max-cache-ttl))))
