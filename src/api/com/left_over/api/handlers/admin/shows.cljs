(ns com.left-over.api.handlers.admin.shows
  (:require
    [com.left-over.api.core :as core]
    [com.left-over.common.services.db.models.shows :as shows]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.utils.logging :as log]
    [com.ben-allred.vow.core :as v]))

(defmulti ^:private handler* (juxt :httpMethod :resource))

(defmethod ^:private handler* ["GET" "/admin/shows"]
  [_]
  (repos/transact shows/select-for-admin))

(defmethod ^:private handler* ["GET" "/admin/shows/{show-id}"]
  [{:keys [pathParameters]}]
  (let [show-id (uuid (:show-id pathParameters))]
    (repos/transact #(shows/find-by-id % show-id))))

(defmethod ^:private handler* ["PUT" "/admin/shows/{show-id}"]
  [{:keys [body pathParameters user]}]
  (let [show-id (uuid (:show-id pathParameters))]
    (repos/transact #(shows/save % user (assoc body :id show-id)))))

(defmethod ^:private handler* ["DELETE" "/admin/shows/{show-id}"]
  [{:keys [pathParameters user]}]
  (let [show-id (uuid (:show-id pathParameters))]
    (v/then (repos/transact #(shows/save % user {:id       show-id
                                                 :deleted? true}))
            (constantly ^{:status 204} {}))))

(defmethod ^:private handler* ["POST" "/admin/shows"]
  [{:keys [body user]}]
  (v/then (repos/transact #(shows/save % user (dissoc body :id)))
          #(with-meta % {:status 201})))

(def handler (core/with-event (core/with-user (core/with-admin-only! handler*))))

(set! (.-exports js/module) #js {:handler handler})
