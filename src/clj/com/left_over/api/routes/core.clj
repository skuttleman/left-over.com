(ns com.left-over.api.routes.core
  (:require
    [clojure.string :as string]
    [com.left-over.api.connectors.dropbox :as dropbox]
    [com.left-over.api.connectors.s3 :as s3]
    [com.left-over.api.services.db.models.locations :as locations]
    [com.left-over.api.services.db.models.shows :as shows]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.api.services.middleware :as middleware]
    [com.left-over.common.services.env :as env]
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

(defn verified? [email pw]
  (some (fn [k]
          (let [[e p] (some-> (env/get k) (string/split #"="))]
            (and e p (= e email) (= p pw))))
        [:ben-logon :john-logon]))

(defn authed! [handler]
  (fn [request]
    (if (:auth/user request)
      (handler request)
      (throw (ex-info "error" {:response {:status 401 :body {:message "You must be authenticated to use this API"}}})))))

(defroutes admin-routes
  (GET "/locations" {:keys [db]}
    {:status 200
     :body   (locations/select-for-admin db)})

  (GET "/shows" {:keys [db]}
    {:status 200
     :body (shows/select-for-admin db)}))

(defroutes app*
  (context "/auth" []
    (GET "/info" {:auth/keys [user]}
      (if user
        {:status 200
         :body   user}
        {:status 401
         :body   {:message "unauthorized"}}))

    ;; TODO: implement OAuth
    (GET "/login" {{email "email" temp-pw "password" uri "redirect-uri"} :query-params :keys [db]}
      (if-let [user (when (verified? email temp-pw) (users/find-by-email db email))]
        (resp/redirect (str uri "?token=" (jwt/encode user)))
        (resp/redirect (str uri "?toast-msg-id=auth/failed")))))

  (context "/api" []
    (context "/admin" []
      (authed! #'admin-routes))

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
      middleware/with-jwt
      middleware/with-ex-handling
      middleware/with-content-type
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
