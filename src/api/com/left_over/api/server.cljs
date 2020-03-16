(ns com.left-over.api.server
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.handlers.admin.locations :as admin.locations]
    [com.left-over.api.handlers.admin.shows :as admin.shows]
    [com.left-over.api.handlers.auth :as auth]
    [com.left-over.api.handlers.pub.images :as pub.images]
    [com.left-over.api.handlers.pub.shows :as pub.shows]
    [com.left-over.api.handlers.pub.videos :as pub.videos]
    [com.left-over.common.services.env :as env]
    [com.left-over.shared.utils.logging :as log :include-macros true]
    [com.left-over.shared.utils.numbers :as numbers]
    cors
    express
    http
    [clojure.string :as string]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.shared.utils.keywords :as keywords])
  (:import
    (goog.string StringBuffer)))

(defn ^:private with-resource [segment handler]
  (fn [req res next]
    (aset req "resource" (str (.-resource req) segment))
    (handler req res next)))

(defn ^:private -use [ctx segment handler]
  (.use ctx segment (with-resource segment handler)))

(defn ^:private -get [ctx segment handler]
  (.get ctx segment (with-resource segment handler)))

(defn ^:private -put [ctx segment handler]
  (.put ctx segment (with-resource segment handler)))

(defn ^:private -post [ctx segment handler]
  (.post ctx segment (with-resource segment handler)))

(defn ^:private -delete [ctx segment handler]
  (.delete ctx segment (with-resource segment handler)))

(defn ^:private body-parser [req _res next]
  (let [buffer (StringBuffer.)]
    (doto req
      (.setEncoding "utf8")
      (.on "data" (fn [chunk]
                    (.append buffer chunk)))
      (.on "end" (fn []
                   (aset req "body" (str buffer))
                   (next)))
      (.on "error" next))))

(defn ^:private handler->route [handler]
  (fn [req res next]
    (let [resource
          (string/replace (.-resource req)
                          #":[a-z_]+"
                          (fn [s & _]
                            (str "{" (string/replace (subs s 1) #"_" "-") "}")))
          params (-> (.-params req)
                     js->clj
                     (->> (maps/map-kv keywords/keyword identity))
                     clj->js)]
      (v/then (handler #js {:headers               (.-headers req)
                            :httpMethod            (.-method req)
                            :body                  (.-body req)
                            :queryStringParameters (.-query req)
                            :pathParameters        params
                            :resource              (cond-> resource
                                                     (and (not= resource "/")
                                                          (string/ends-with? resource "/"))
                                                     (subs 0 (dec (count resource))))
                            :path                  (.-fullPath req)}
                       nil)
              (fn [result]
                (doto res
                  (.status (.-statusCode result))
                  (.set (.-headers result))
                  (.send (.-body result))))
              next))))

(def ^:private cors-middleware (cors #js {:origin (fn [_ cb] (cb nil true))}))

(def ^:private pub-route
  (doto (express/Router)
    (-get "/images" (handler->route pub.images/handler))
    (-get "/shows" (handler->route pub.shows/handler))
    (-get "/videos" (handler->route pub.videos/handler))))

(def ^:private auth-route
  (doto (express/Router)
    (-get "/callback" (handler->route auth/handler))
    (-get "/info" (handler->route auth/handler))
    (-get "/login" (handler->route auth/handler))))

(def ^:private admin-locations
  (doto (express/Router)
    (-post "/" (handler->route admin.locations/handler))
    (-get "/" (handler->route admin.locations/handler))
    (-put "/:location_id" (handler->route admin.locations/handler))))

(def ^:private admin-shows
  (doto (express/Router)
    (-get "/" (handler->route admin.shows/handler))
    (-post "/" (handler->route admin.shows/handler))
    (-delete "/:show_id" (handler->route admin.shows/handler))
    (-put "/:show_id" (handler->route admin.shows/handler))
    (-get "/:show_id" (handler->route admin.shows/handler))))

(def ^:private admin-route
  (doto (express/Router)
    (-use "/locations" admin-locations)
    (-use "/shows" admin-shows)))

(def ^:private app
  (doto (express)
    (.use (fn [req _res next]
            (aset req "fullPath" (.-path req))
            (next)))
    (.options "*" (fn [req res next]
                    (.set res "Access-Control-Allow-Credentials" "true")
                    (cors-middleware req res next)))
    (.use cors-middleware)
    (.use body-parser)
    (-use "/public" pub-route)
    (-use "/auth" auth-route)
    (-use "/admin" admin-route)))

(defonce server
  (let [port (numbers/parse-int (env/get :dev-aws-port "3100"))]
    (doto (http/createServer app)
      (.listen port #(log/info "The server is listening on PORT" port)))))
