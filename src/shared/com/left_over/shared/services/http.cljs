(ns com.left-over.shared.services.http
  (:refer-clojure :exclude [get])
  (:require
    [cljs-http.client :as client]
    [cljs-http.core :as http*]
    [clojure.core.async :as async]
    [clojure.set :as set]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.json :as json]
    [com.left-over.shared.utils.logging :as log :include-macros true]
    [com.left-over.shared.utils.maps :as maps]))

(def status->kw
  {200 ::ok
   201 ::created
   202 ::accepted
   203 ::non-authoritative-information
   204 ::no-content
   205 ::reset-content
   206 ::partial-content
   300 ::multiple-choices
   301 ::moved-permanently
   302 ::found
   303 ::see-other
   304 ::not-modified
   305 ::use-proxy
   306 ::unused
   307 ::temporary-redirect
   400 ::bad-request
   401 ::unauthorized
   402 ::payment-required
   403 ::forbidden
   404 ::not-found
   405 ::method-not-allowed
   406 ::not-acceptable
   407 ::proxy-authentication-required
   408 ::request-timeout
   409 ::conflict
   410 ::gone
   411 ::length-required
   412 ::precondition-failed
   413 ::request-entity-too-large
   414 ::request-uri-too-long
   415 ::unsupported-media-type
   416 ::requested-range-not-satisfiable
   417 ::expectation-failed
   500 ::internal-server-error
   501 ::not-implemented
   502 ::bad-gateway
   503 ::service-unavailable
   504 ::gateway-timeout
   505 ::http-version-not-supported})

(def kw->status
  (set/map-invert status->kw))

(defn ^:private check-status [lower upper response]
  (let [status (or (when (vector? response)
                     (kw->status (first response)))
                   (as-> response $
                         (cond-> $ (vector? $) second)
                         (:status $)
                         (cond-> $ (keyword? $) kw->status))
                   500)]
    (<= lower status upper)))

(def ^{:arglists '([response])} success?
  (partial check-status 200 299))

(def ^{:arglists '([response])} client-error?
  (partial check-status 400 499))

(def ^{:arglists '([response])} server-error?
  (partial check-status 500 599))

(def ^{:arglists '([response])} error?
  (some-fn client-error? server-error?))

(def ^:private client*
  (-> http*/request
      client/wrap-query-params
      client/wrap-basic-auth
      client/wrap-oauth
      client/wrap-url
      client/wrap-accept
      client/wrap-content-type
      client/wrap-form-params
      client/wrap-method
      client/wrap-multipart-params
      client/wrap-channel-from-request-map))

(defn ^:private client [request]
  (let [token (when (:token? request)
                (some-> js/localStorage (.getItem "auth-token")))]
    (-> request
        (cond->
          token
          (assoc-in [:headers "authorization"] (str "Bearer " token))

          (not token)
          (assoc :with-credentials? false))
        client*)))

(defn ^:private prep [request content-type]
  (-> request
      (update :headers (partial into {} (map (juxt (comp name key) val))))
      (maps/update-maybe :body (fn [body]
                                 (if (string? body)
                                   body
                                   (case content-type
                                     "application/json" (json/stringify body)
                                     "application/edn" (edn/stringify body)
                                     body))))))

(defn ^:private request* [chan]
  (async/go
    (let [ch-response (async/<! chan)
          response (-> (if-let [data (ex-data ch-response)]
                         data
                         ch-response)
                       (update :headers (partial into {} (map (juxt (comp keyword key) val)))))
          content-type (get-in response [:headers :content-type])]
      (-> response
          (update :status #(status->kw % %))
          (update :body (fn [body]
                          (condp re-find (str content-type)
                            #"application/json" (json/parse body)
                            #"application/edn" (edn/parse body)
                            body)))
          (->> (conj [(if (success? response) :success :error)]))))))

(defn ^:private response*
  ([response?]
   (fn [value]
     (response* value response?)))
  ([value response?]
   (cond-> value (not response?) :body)))

(defn ^:private from-ch [ch]
  (-> ch
      (v/ch->prom (comp #{:success} first))
      (v/then second (comp v/reject second))))

(defn ^:private go [method url {:keys [response? json?] :as request}]
  (let [content-type (if json? "application/json" "application/edn")
        headers (merge (cond-> {:accept content-type}
                         (:body request) (assoc :content-type content-type))
                       (:headers request))]
    (-> request
        (assoc :method method :url url :headers headers)
        (prep (:content-type headers))
        client
        request*
        from-ch
        (v/catch (comp v/reject (response* response?)))
        (v/then-> (response* response?)))))

(defn get
  ([url]
   (get url nil))
  ([url request]
   (go :get url request)))

(defn post [url request]
  (go :post url request))

(defn patch [url request]
  (go :patch url request))

(defn put [url request]
  (go :put url request))

(defn delete
  ([url]
   (delete url nil))
  ([url request]
   (go :delete url request)))
