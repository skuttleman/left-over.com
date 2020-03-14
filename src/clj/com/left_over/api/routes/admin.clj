(ns com.left-over.api.routes.admin
  (:require
    [com.left-over.common.services.db.models.locations :as locations]
    [com.left-over.api.utils.promises :as prom]
    [com.left-over.common.services.db.models.shows :as shows]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]])
  (:import
    (java.util UUID)))

(defroutes routes*
  (PUT "/locations/:location-id" {:keys [auth/user body db params]}
    {:status 200
     :body   (prom/deref! (locations/save db user (assoc body :id (UUID/fromString (:location-id params)))))})
  (POST "/locations" {:keys [auth/user body db]}
    {:status 201
     :body   (prom/deref! (locations/save db user body))})
  (GET "/locations" {:keys [db]}
    {:status 200
     :body   (prom/deref! (locations/select-for-admin db))})
  (GET "/shows/:show-id" {:keys [db params]}
    (if-let [show (prom/deref! (shows/find-by-id db (UUID/fromString (:show-id params))))]
      {:status 200
       :body   show}
      {:status 404
       :body   {:message "not found"}}))
  (PUT "/shows/:show-id" {:keys [auth/user body db params]}
    {:status 200
     :body   (prom/deref! (shows/save db user (assoc body :id (UUID/fromString (:show-id params)))))})
  (DELETE "/shows/:show-id" {:keys [auth/user db params]}
    (prom/deref! (shows/save db user {:id (UUID/fromString (:show-id params)) :deleted? true}))
    {:status 204})
  (POST "/shows" {:keys [auth/user body db]}
    {:status 201
     :body   (prom/deref! (shows/save db user body))})
  (GET "/shows" {:keys [db]}
    {:status 200
     :body   (prom/deref! (shows/select-for-admin db))}))

(defn routes [request]
  (if (:auth/user request)
    (routes* request)
    (throw (ex-info "error" {:response {:status 401 :body {:message "You must be authenticated to use this API"}}}))))
