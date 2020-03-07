(ns com.left-over.common.services.http
  (:refer-clojure :exclude [get])
  (:require
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [#?(:clj clj-http.core :cljs cljs-http.core) :as http*]
    [clojure.core.async :as async]
    [clojure.set :as set]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [com.left-over.common.services.env :as env]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.json :as json]
    [com.left-over.common.utils.logging :as log]))

(def status->kw
  {200 :http.status/ok
   201 :http.status/created
   202 :http.status/accepted
   203 :http.status/non-authoritative-information
   204 :http.status/no-content
   205 :http.status/reset-content
   206 :http.status/partial-content
   300 :http.status/multiple-choices
   301 :http.status/moved-permanently
   302 :http.status/found
   303 :http.status/see-other
   304 :http.status/not-modified
   305 :http.status/use-proxy
   306 :http.status/unused
   307 :http.status/temporary-redirect
   400 :http.status/bad-request
   401 :http.status/unauthorized
   402 :http.status/payment-required
   403 :http.status/forbidden
   404 :http.status/not-found
   405 :http.status/method-not-allowed
   406 :http.status/not-acceptable
   407 :http.status/proxy-authentication-required
   408 :http.status/request-timeout
   409 :http.status/conflict
   410 :http.status/gone
   411 :http.status/length-required
   412 :http.status/precondition-failed
   413 :http.status/request-entity-too-large
   414 :http.status/request-uri-too-long
   415 :http.status/unsupported-media-type
   416 :http.status/requested-range-not-satisfiable
   417 :http.status/expectation-failed
   500 :http.status/internal-server-error
   501 :http.status/not-implemented
   502 :http.status/bad-gateway
   503 :http.status/service-unavailable
   504 :http.status/gateway-timeout
   505 :http.status/http-version-not-supported})

(def kw->status
  (set/map-invert status->kw))

(defn ^:private check-status [lower upper response]
  (let [status (or (when (vector? response)
                     (kw->status (first response)))
                   (as-> response $
                         (cond-> $ (vector? $) second)
                         (:status $)
                         (cond-> $ (keyword? $) kw->status))
                   (log/warn "unknown status" response)
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
      #?@(:clj  [client/wrap-request-timing
                 client/wrap-decompression
                 client/wrap-input-coercion
                 client/wrap-user-info
                 client/wrap-additional-header-parsing
                 client/wrap-output-coercion
                 client/wrap-exceptions
                 client/wrap-nested-params
                 client/wrap-accept-encoding
                 client/wrap-flatten-nested-params
                 client/wrap-unknown-host]
          :cljs [client/wrap-multipart-params
                 client/wrap-channel-from-request-map])))

(defn ^:private client [request]
  #?(:clj  (let [cs (clj-http.cookies/cookie-store)
                 ch (async/chan)]
             (-> request
                 (merge {:async? true :cookie-store cs})
                 (client* (fn [response]
                            (async/put! ch (assoc response :cookies (clj-http.cookies/get-cookies cs))))
                          (fn [exception]
                            (async/put! ch (assoc (ex-data exception) :cookies (clj-http.cookies/get-cookies cs))))))
             ch)
     :cljs (let [token (when (env/get :admin?)
                         (.getItem js/localStorage "auth-token"))]
             (-> request
                 (cond->
                   token
                   (assoc-in [:headers "authorization"] (str "Bearer " token))

                   (not token)
                   (assoc :with-credentials? false))
                 client*))))

(defn ^:private prep [request content-type]
  (-> request
      (update :headers (partial into {} (map (juxt (comp name key) val))))
      (cond->
        (:body request) (update :body (fn [body]
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
                          (case content-type
                            "application/json" (json/parse body)
                            "application/edn" (edn/parse body)
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

(defn ^:private go [method url {:keys [response?] :as request}]
  (let [content-type #?(:cljs "application/edn" :default "application/json")
        headers (merge {:content-type content-type :accept content-type}
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
