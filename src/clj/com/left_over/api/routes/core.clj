(ns com.left-over.api.routes.core
  (:require
    [com.left-over.api.connectors.dropbox :as dropbox]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.reload :refer [wrap-reload]]))

(defroutes app*
  (context "/api" []
    (GET "/photos" []
      (let [[status photos] @(dropbox/fetch-photos)]
        (if (= :success status)
          {:status  200
           :body    (pr-str photos)
           :headers {"content-type" "application/edn"}}
          {:status 500
           :body "something went wrong"}))))
  (context "/" []
    (ANY "/*" [] {:status 404 :body "not found"})))

(def app
  (-> #'app*
      wrap-multipart-params
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-cookies
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :options])))

(def app-dev
  (-> #'app
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})))
