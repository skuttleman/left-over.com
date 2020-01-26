(ns com.left-over.api.routes.core
  (:require
    [com.left-over.api.connectors.dropbox :as dropbox]
    [com.left-over.api.connectors.s3 :as s3]
    [com.left-over.api.services.db.models.shows :as shows]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.api.services.middleware :as middleware]
    [com.left-over.common.utils.logging :as log]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.util.response :as resp]))

(defroutes app*
  (context "/auth" []
    (GET "/info" {:auth/keys [user]}
      (if user
        {:status 200
         :body   user}
        {:status 401
         :body   {:message "unauthorized"}}))
    (GET "/login" {:keys [db query-params] :as req}
      (if-let [uri (get query-params "redirect-uri")]
        (resp/redirect (str uri "?token=" (jwt/encode (first (users/select-all db)))))
        {:status 400
         :body   {:message "must provide redirect-uri"}})))
  (context "/api" []
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

  (context "/" []
    (ANY "/*" [] {:status 404 :body {:message "not found"}})))

(def app
  (-> #'app*
      middleware/with-transaction
      middleware/with-content-type
      middleware/with-jwt
      wrap-multipart-params
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-cookies
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :head :options]
                 :access-control-allow-credentials "true")))

(def app-dev
  (-> #'app
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})))
