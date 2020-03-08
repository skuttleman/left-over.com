(ns com.left-over.api.routes.public
  (:require
    [com.left-over.api.services.db.models.shows :as shows]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]))

(defroutes routes
  (GET "/shows" {:keys [db]}
    {:status 200
     :body   (shows/select-for-website db)}))
