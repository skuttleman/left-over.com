(ns com.left-over.api.routes.public
  (:require
    [com.left-over.api.connectors.dropbox :as dropbox]
    [com.left-over.api.connectors.s3 :as s3]
    [com.left-over.api.services.db.models.shows :as shows]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]))

(defroutes routes
  (GET "/photos" []
    (let [[status photos] @(dropbox/fetch-photos)]
      (if (= :success status)
        {:status 200
         :body   photos}
        {:status 500
         :body   "something went wrong"})))
  (GET "/images/:image-key" [image-key]
    (if-let [s3-object (s3/fetch (str "images/" image-key))]
      {:status  200
       :headers {"content-type"   (:content-type s3-object)
                 "content-length" (:content-length s3-object)}
       :body    (:body s3-object)}
      {:status 404}))
  (context "/shows" []
    (GET "/" {:keys [db]}
      {:status 200
       :body   (shows/select-for-website db)})))
