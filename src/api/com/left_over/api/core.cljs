(ns com.left-over.api.core
  (:require
    [cljs.nodejs :as nodejs]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.common.utils.keywords :as keywords]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.common.utils.maps :as maps]))

(nodejs/enable-util-print!)
(set! (.-XMLHttpRequest js/global) (.-XMLHttpRequest (nodejs/require "xmlhttprequest")))
(aset js/global "localStorage" nil)

(defn ^:private redirect? [status]
  (and status (<= 300 status 399)))

(defn ^:private response [default-status event]
  (let [origin (get-in event [:headers :origin])]
    (fn [body]
      (let [{:keys [status headers]} (meta body)]
        {:statusCode (or status default-status)
         :body       (when-not (redirect? status) (pr-str body))
         :headers    (cond-> {:Access-Control-Allow-Credentials "true"
                              :Content-Type                     "application/edn"}
                       origin (assoc :Access-Control-Allow-Origin origin)
                       headers (merge headers))}))))

(defn ^:private error [err]
  (log/error err)
  (if-let [response (:response (ex-data err))]
    (with-meta (:body response) response)
    {:message "error fetching resource"}))

(defn with-event [handler]
  (fn handle
    ([event ctx cb]
     (v/then (handle event ctx) (partial cb nil) cb))
    ([event _ctx]
     (let [event* (update (js->clj event :keywordize-keys true)
                          :headers
                          (partial maps/map-kv (comp keywords/keyword name) identity))]
       (-> event*
           handler
           (v/then (response 200 event*))
           (v/catch (comp (response 500 event*) error))
           (v/then clj->js))))))

(defn with-user [handler]
  (fn [event]
    (let [user (try
                    (some-> event (get-in [:headers :authorization]) (subs 7) jwt/decode :data)
                    (catch :default _ nil))]
      (handler (cond-> event
                 user (assoc :user user))))))
