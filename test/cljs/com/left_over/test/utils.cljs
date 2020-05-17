(ns com.left-over.test.utils
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.vow.core :as v]
    [com.left-over.shared.services.protocols :as p]))

(defprotocol IReInit
  (init! [this]))

(defn ->HttpClient [responses]
  (let [requests (atom [])]
    (reify
      p/IHttpClient
      (go [_ method uri request]
        (swap! requests conj [method uri request])
        (let [response (get responses [method uri])]
          (cond
            (v/promise? response) response
            (fn? response) (v/vow (response))
            :else (v/resolve response))))

      IReInit
      (init! [_]
        (reset! requests []))

      IDeref
      (-deref [_]
        @requests))))

(defn ->DbConn [results]
  (let [queries (atom [])
        pos (atom 0)]
    (reify p/IDbConnection
      (query [_ _ _]
        (let [result (nth results @pos)]
          (swap! pos inc)
          (cond
            (v/promise? result) result
            (fn? result) (v/vow (result))
            :else (v/resolve (clj->js result)))))
      (prepare [_ query]
        (swap! queries conj query)
        [(name (gensym "QUERY")) :param1 :param2])

      IReInit
      (init! [_]
        (reset! queries [])
        (reset! pos 0))

      IDeref
      (-deref [_]
        @queries))))

(defn ->OAuthProvider [impl]
  (let [calls (atom [])]
    (reify p/IOauthProvider
      (profile [_ token-info]
        (swap! calls conj [:profile token-info])
        (v/vow (get impl :profile (v/reject))))
      (token [_ params]
        (swap! calls conj [:token params])
        (v/vow (get impl :token (v/reject))))
      (redirect-uri [_ state]
        (swap! calls conj [:redirect-uri state])
        (get impl :redirect-uri "some-uri"))

      IReInit
      (init! [_]
        (reset! calls []))

      IDeref
      (-deref [_]
        @calls))))

(defn prom->ch [prom]
  (let [ch (async/promise-chan)]
    (v/peek prom (partial async/put! ch))
    ch))

(defn returning [& results]
  (let [state (atom (cycle results))]
    (fn [& _]
      (let [result (first @state)]
        (swap! state rest)
        result))))
