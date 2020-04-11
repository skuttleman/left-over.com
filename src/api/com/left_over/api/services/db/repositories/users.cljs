(ns com.left-over.api.services.db.repositories.users
  (:require
    [com.left-over.api.services.db.entities :as entities]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.maps :as maps]))

(defmethod repos/->api ::model
  [_ user]
  (maps/update-in-maybe user [:token-info :expires_at] dates/parse))

(defn select-by [clause]
  (-> entities/users
      entities/select
      (entities/with-alias :users)
      (cond-> clause (assoc :where clause))))

(defn merge-token-info [token-info clause]
  (-> entities/users
      (entities/modify clause)
      (entities/json-merge :token-info token-info)))
