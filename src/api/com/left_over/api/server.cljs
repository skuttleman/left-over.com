(ns com.left-over.api.server
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.handlers.public.shows :as shows]
    [com.left-over.api.handlers.public.images :as images]
    [com.left-over.api.handlers.public.videos :as videos]
    [com.left-over.api.services.env :as env]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.logging :as log :include-macros true]
    [com.left-over.common.utils.numbers :as numbers]
    cors
    express
    http)
  (:import
    (goog.string StringBuffer)))

(defn body-parser [req _res next]
  (let [buffer (StringBuffer.)]
    (doto req
      (.setEncoding "utf8")
      (.on "data" (fn [chunk]
                    (.append buffer chunk)))
      (.on "end" (fn []
                   (aset req "body" (edn/parse (str buffer)))
                   (next)))
      (.on "error" next))))

(defn handler->route [handler]
  (fn [_req res next]
    (v/then (handler nil nil)
            (fn [result]
              (doto res
                (.status (.-statusCode result))
                (.set (.-headers result))
                (.send (.-body result))))
            next)))

(def app
  (doto (express)
    (.use (cors))
    (.use body-parser)
    (.get "/public/images" (handler->route images/handler))
    (.get "/public/shows" (handler->route shows/handler))
    (.get "/public/videos" (handler->route videos/handler))))

(defonce server
  (let [port (numbers/parse-int (env/get :dev-aws-port "3100"))]
    (doto (http/createServer app)
      (.listen port (fn [] (log/info "The server is listening on PORT" port))))))
