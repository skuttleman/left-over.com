(ns com.left-over.api.routes.core
  (:require
    [com.left-over.api.routes.admin :as admin]
    [com.left-over.api.routes.auth :as auth]
    [com.left-over.api.routes.public :as public]
    [com.left-over.api.services.middleware :as middleware]
    [compojure.core :refer [ANY context defroutes]]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.reload :refer [wrap-reload]]))

(defroutes app*
  (context "/auth" []
    #'auth/routes)
  (context "/api" []
    (context "/admin" []
      #'admin/routes)
    #'public/routes)
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
                 :access-control-allow-methods [:get :post :put :delete :head :options]
                 :access-control-allow-credentials "true")))

(def app-dev
  (-> #'app
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})))
