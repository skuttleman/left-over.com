(ns com.left-over.api.server
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.handlers.auth :as auth]
    [com.left-over.api.handlers.pub.images :as images]
    [com.left-over.api.handlers.pub.shows :as shows]
    [com.left-over.api.handlers.pub.videos :as videos]
    [com.left-over.api.services.env :as env]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.logging :as log :include-macros true]
    [com.left-over.common.utils.numbers :as numbers]
    cors
    express
    http)
  (:import
    (goog.string StringBuffer)))

(defn ^:private body-parser [req _res next]
  (let [buffer (StringBuffer.)]
    (doto req
      (.setEncoding "utf8")
      (.on "data" (fn [chunk]
                    (.append buffer chunk)))
      (.on "end" (fn []
                   (aset req "body" (edn/parse (str buffer)))
                   (next)))
      (.on "error" next))))

(defn ^:private handler->route [handler]
  (fn [req res next]
    (v/then (handler #js {:headers               (.-headers req)
                          :body                  (.-body req)
                          :queryStringParameters (.-query req)
                          :path                  (str "/auth" (.-path req))}
                     nil)
            (fn [result]
              (doto res
                (.status (.-statusCode result))
                (.set (.-headers result))
                (.send (.-body result))))
            next)))

(def ^:private cors-middleware (cors #js {:origin (fn [_ cb] (cb nil true))}))

(def ^:private pub-route
  (doto (express/Router)
    (.get "/images" (handler->route images/handler))
    (.get "/shows" (handler->route shows/handler))
    (.get "/videos" (handler->route videos/handler))))

(def ^:private auth-route
  (doto (express/Router)
    (.get "/callback" (handler->route auth/handler))
    (.get "/info" (handler->route auth/handler))
    (.get "/login" (handler->route auth/handler))))

(def ^:private app
  (doto (express)
    (.options "*" (fn [req res next]
                    (.set res "Access-Control-Allow-Credentials" "true")
                    (cors-middleware req res next)))
    (.use cors-middleware)
    (.use body-parser)
    (.use "/public" pub-route)
    (.use "/auth" auth-route)))

(defonce server
  (let [port (numbers/parse-int (env/get :dev-aws-port "3100"))]
    (doto (http/createServer app)
      (.listen port (fn [] (log/info "The server is listening on PORT" port))))))
