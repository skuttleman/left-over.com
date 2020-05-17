(ns com.left-over.api.handlers.admin.shows
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.core :as core]
    [com.left-over.api.services.db.models.shows :as shows]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.google :as google]
    [com.left-over.shared.services.http :as http]
    [com.left-over.shared.utils.logging :as log]))

(defmulti ^:private handler* (juxt :httpMethod :resource))

(defmethod ^:private handler* ["GET" "/admin/shows"]
  [_]
  (repos/transact shows/select-for-admin))

(defmethod ^:private handler* ["GET" "/admin/shows/{show-id}"]
  [{:keys [pathParameters]}]
  (let [show-id (uuid (:show-id pathParameters))]
    (v/then-> (repos/transact shows/find-by-id show-id)
              (or ^{:status 404} {}))))

(defmethod ^:private handler* ["PUT" "/admin/shows/{show-id}"]
  [{:keys [body pathParameters user]}]
  (let [show-id (uuid (:show-id pathParameters))]
    (repos/transact shows/save user (assoc body :id show-id))))

(defmethod ^:private handler* ["DELETE" "/admin/shows"]
  [{:keys [body]}]
  (v/and (repos/transact shows/delete (:show-ids body))
         ^{:status 204} {}))

(defmethod ^:private handler* ["POST" "/admin/shows"]
  [{:keys [body user]}]
  (v/then-> (repos/transact shows/save user (dissoc body :id))
            (with-meta {:status 201})))

(defmethod ^:private handler* ["POST" "/admin/calendar/merge"]
  [{:keys [user]}]
  (repos/transact (fn [conn]
                    (let [client (http/->client)
                          oauth (google/->oauth client)]
                      (v/and (-> conn
                                 (google/fetch-calendar-events client oauth (:id user))
                                 (v/then-> (->> (shows/refresh-events conn user)))
                                 (v/peek nil (fn [err]
                                               (log/error "Failed to refresh calendar events" err))))
                             ^{:status 204} [])))))

(defmethod ^:private handler* ["GET" "/admin/calendar/merge"]
  [_]
  (repos/transact shows/select-unmerged))

(def handler (core/with-event (core/with-user (core/with-admin-only! handler*))))

(set! (.-exports js/module) #js {:handler handler})
