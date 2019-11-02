(ns com.left-over.api.routes.core
  (:require
    [com.left-over.api.connectors.dropbox :as dropbox]
    [com.left-over.api.connectors.facebook :as fb]
    [com.left-over.api.connectors.s3 :as s3]
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
           :body   "something went wrong"})))

    (GET "/images/:image-key" [image-key]
      (if-let [s3-object (s3/fetch (str "images/" image-key))]
        {:status  200
         :headers {"content-type"   (:content-type s3-object)
                   "content-length" (:content-length s3-object)}
         :body    (:body s3-object)}
        {:status 404}))

    (context "/shows" []
      (GET "/past" []
        {:status  200
         :body    (pr-str (fb/past-shows))
         :headers {"content-type" "application/edn"}})

      (GET "/upcoming" []
        {:status  200
         :body    (pr-str (fb/upcoming-shows))
         :headers {"content-type" "application/edn"}})))

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
                 :access-control-allow-methods [:get :head :options])))

(def app-dev
  (-> #'app
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})))
