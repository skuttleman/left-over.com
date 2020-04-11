(ns com.left-over.api.handlers.admin.locations
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.core :as core]
    [com.left-over.api.services.db.models.locations :as locations]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.logging :as log]))

(defmulti ^:private handler* (juxt :httpMethod :resource))

(defmethod ^:private handler* ["GET" "/admin/locations"]
  [_]
  (repos/transact locations/select-for-admin))

(defmethod ^:private handler* ["POST" "/admin/locations"]
  [{:keys [body user]}]
  (repos/transact (fn [conn]
                    (-> conn
                        (locations/save user (dissoc body :id))
                        (v/then-> (with-meta {:status 201}))))))

(defmethod ^:private handler* ["PUT" "/admin/locations/{location-id}"]
  [{:keys [body pathParameters user]}]
  (repos/transact locations/save user (assoc body :id (uuid (:location-id pathParameters)))))

(def handler (core/with-event (core/with-user (core/with-admin-only! handler*))))

(set! (.-exports js/module) #js {:handler handler})
