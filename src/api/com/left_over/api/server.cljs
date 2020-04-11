(ns com.left-over.api.server
  (:require
    [bidi.bidi :as bidi]
    [clojure.set :as set]
    [clojure.string :as string]
    [com.ben-allred.espresso.core :as es]
    [com.ben-allred.espresso.middleware :as esmw]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.handlers.admin.locations :as admin.locations]
    [com.left-over.api.handlers.admin.shows :as admin.shows]
    [com.left-over.api.handlers.auth :as auth]
    [com.left-over.api.handlers.pub.images :as pub.images]
    [com.left-over.api.handlers.pub.shows :as pub.shows]
    [com.left-over.api.handlers.pub.videos :as pub.videos]
    [com.left-over.api.services.env :as env]
    [com.left-over.shared.utils.logging :as log :include-macros true]
    [com.left-over.shared.utils.numbers :as numbers]
    [com.left-over.shared.utils.uri :as uri]))

(defprotocol IServer
  (start! [this])
  (stop! [this]))

(def ^:private routes
  ["" {"/public" {"/images" {:get :public.images/get}
                  "/shows"  {:get :public.shows/get}
                  "/videos" {:get :public.videos/get}}
       "/auth"   {"/callback" {:get :auth.callback/get}
                  "/info"     {:get :auth.info/get}
                  "/login"    {:get :auth.login/get}}
       "/admin"  {"/locations" {""                 {:post :admin.locations/post
                                                    :get  :admin.locations/get}
                                ["/" :location-id] {:put :admin.location/put}}
                  "/shows"     {""             {:post   :admin.shows/post
                                                :get    :admin.shows/get
                                                :delete :admin.shows/delete}
                                ["/" :show-id] {:get :admin.show/get
                                                :put :admin.show/put}}
                  "/calendar"  {"/merge" {:post :admin.calendar.merge/post
                                          :get  :admin.calendar.merge/get}}}}])

(defn ^:private handle* [handler request]
  (-> request
      (update :method (comp string/upper-case name))
      (set/rename-keys {:method       :httpMethod
                        :query-params :queryStringParameters
                        :route-params :pathParameters})
      (assoc :resource (bidi/path-for routes (:bidi/route request)
                                      :show-id "{show-id}"
                                      :location-id "{location-id}"))
      clj->js
      (handler nil)
      (v/then-> (js->clj :keywordize-keys true)
                (set/rename-keys {:statusCode :status}))))

(defn handler [request]
  (-> request
      :bidi/route
      (case
        (:admin.calendar.merge/get :admin.calendar.merge/post :admin.show/get
         :admin.show/put :admin.shows/delete :admin.shows/get :admin.shows/post)
        admin.shows/handler

        (:admin.locations/get :admin.locations/post :admin.location/put)
        admin.locations/handler

        (:auth.callback/get :auth.info/get :auth.login/get)
        auth/handler

        :public.images/get
        pub.images/handler

        :public.shows/get
        pub.shows/handler

        :public.videos/get
        pub.videos/handler)
      (handle* request)))

(defn ^:private with-routing [handler routes]
  (fn [request]
    (if-let [{route :handler :keys [route-params]} (bidi/match-route routes
                                                                     (:path request)
                                                                     :request-method
                                                                     (:method request))]
      (handler (assoc request :bidi/route route :route-params route-params))
      (handler request))))

(defn ^:private with-query-params
  ([handler]
   (with-query-params handler false))
  ([handler keywordize?]
   (fn [request]
     (->> (string/split (:query-string request) #"&") (remove empty?)
          (reduce (fn [params param]
                    (let [[k v] (string/split param #"=")
                          k (cond-> k keywordize? keyword)
                          v (if (nil? v) true (uri/url-decode v))]
                      (assoc params k v)))
                  {})
          (assoc request :query-params)
          handler))))

(defn ^:private with-cors [handler]
  (fn [{:keys [headers] :as request}]
    (let [origin (:origin headers)
          req-headers (:access-control-request-headers headers)
          response-headers (cond-> {"Access-Control-Allow-Credentials" "true"
                                    "Access-Control-Allow-Methods"     "GET,POST,PUT,PATCH,DELETE,HEAD"}
                             origin (assoc "Access-Control-Allow-Origin" origin)
                             req-headers (assoc "Access-Control-Allow-Headers" (if (string? req-headers)
                                                                                 req-headers
                                                                                 (string/join "," req-headers))))]
      (if (= :options (:method request))
        (v/resolve {:status  204
                    :headers response-headers})
        (-> request
            handler
            (v/then-> (update :headers merge response-headers)))))))

(defn ^:private with-keyword-headers [handler]
  (fn [request]
    (-> request
        (update :headers (partial into {} (map (juxt (comp keyword key) val))))
        handler)))

(defn ^:private with-dev-debug [handler]
  (fn [request]
    (-> request
        handler
        (v/peek nil #(log/spy ["ERROR" %])))))

(def ^:private app (-> handler
                       (with-routing routes)
                       esmw/with-body
                       with-query-params
                       with-cors
                       with-keyword-headers
                       with-dev-debug))

(defrecord ReloadableServer [server]
  IServer
  (start! [_]
    (.listen (reset! server (es/create-server app))
             (numbers/parse-int (env/get :dev-aws-port "3100"))
             #(log/info "The server is listening on PORT" (env/get :dev-aws-port "3100"))))
  (stop! [_]
    (.close @server)
    (log/info "The server has stopped.")))

(defonce server
  (doto (->ReloadableServer (atom nil))
    (start!)))

(defn ^:export restart! []
  (stop! server)
  (start! server))
