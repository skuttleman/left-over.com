(ns com.left-over.api.core
  (:require
    [cljs.nodejs :as nodejs]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.shared.utils.keywords :as keywords]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.shared.utils.json :as json]
    [com.left-over.shared.utils.edn :as edn]))

(nodejs/enable-util-print!)
(set! (.-XMLHttpRequest js/global) (.-XMLHttpRequest (nodejs/require "xmlhttprequest")))
(aset js/global "localStorage" nil)

(defn ^:private nobody? [status]
  (and status (or (= status 204)
                  (<= 300 status 399))))

(defn ^:private response [default-status event]
  (let [origin (get-in event [:headers :origin])]
    (fn [body]
      (let [{:keys [status headers]} (meta body)]
        {:statusCode (or status default-status)
         :body       (when-not (nobody? status) (pr-str body))
         :headers    (cond-> {:Access-Control-Allow-Credentials "true"
                              :Content-Type                     "application/edn"}
                       origin (assoc :Access-Control-Allow-Origin origin)
                       headers (merge headers))}))))

(defn ^:private error [err]
  (log/error err)
  (if-let [response (:response (ex-data err))]
    (with-meta (:body response) response)
    {:message "error fetching resource"}))

(defn ^:private log [m msg]
  (log/info msg (select-keys m #{:headers :httpMethod :path :pathParameters
                                 :queryStringParameters :statusCode}))
  m)

(defn ^:private parse [body content-type]
  (condp re-matches (str content-type)
    #"application/edn" (edn/parse body)
    #"application/json" (json/parse body)
    body))

(defn with-event [handler]
  (fn handle
    ([event ctx cb]
     (v/then (handle event ctx) (partial cb nil) cb))
    ([event _ctx]
     (let [event* (update (js->clj event :keywordize-keys true)
                          :headers
                          (partial maps/map-kv (comp keywords/keyword name) identity))
           event* (maps/update-maybe event* :body parse (get-in event* [:headers :content-type]))]
       (-> event*
           (log "REQUEST:")
           handler
           (v/then (response 200 event*))
           (v/catch (comp (response 500 event*) error))
           (v/then-> (log "RESPONSE:") clj->js))))))

(defn with-user [handler]
  (fn [event]
    (let [user (try
                 (some-> event (get-in [:headers :authorization]) (subs 7) jwt/decode :data)
                 (catch :default _ nil))]
      (handler (cond-> event
                 user (assoc :user user))))))

(defn with-admin-only! [handler]
  (fn [event]
    (if (:user event)
      (handler event)
      (v/reject (ex-info "" {:response {:status 401 :body {:message "unauthorized"}}})))))
