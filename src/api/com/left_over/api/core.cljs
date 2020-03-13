(ns com.left-over.api.core
  (:require
    [cljs.nodejs :as nodejs]
    [com.ben-allred.vow.core :as v]))

(nodejs/enable-util-print!)
(set! (.-XMLHttpRequest js/global) (.-XMLHttpRequest (nodejs/require "xmlhttprequest")))
(aset js/global "localStorage" nil)

(defn ^:private response [status event]
  (let [origin (get-in event [:headers :origin])]
    (fn [body]
      {:statusCode status
       :body       (pr-str body)
       :headers    (cond-> {:Access-Control-Allow-Credentials "true"
                            :Content-Type                     "application/edn"}
                     origin (assoc :Access-Control-Allow-Origin origin))})))

(defn ^:private error [err]
  (js/console.error err)
  {:message "error fetching resource"})

(defn handler [fetch-fn]
  (fn handle
    ([event ctx cb]
     (v/then (handle event ctx) (partial cb nil) cb))
    ([event _ctx]
     (let [event* (js->clj event :keywordize-keys true)]
       (-> (fetch-fn)
           (v/then (response 200 event*))
           (v/catch (comp (response 500 event*) error))
           (v/then clj->js))))))
