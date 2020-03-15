(ns com.left-over.ui.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.left-over.ui.services.env :as env]
    [com.left-over.common.utils.keywords :as keywords]
    [com.left-over.common.utils.maps :as maps]
    [com.left-over.ui.services.store.core :as store]
    [pushy.core :as pushy]))

(defn ^:private routes [& routes]
  ["" (into [] (concat routes [[true :nav/not-found]]))])

(def ^:private api-routes
  (routes ["/api"
           [["/shows" :api/shows]
            ["/admin"
             [[["/locations/" :location-id] :api.admin/location]
              ["/locations" :api.admin/locations]
              [["/shows/" :show-id] :api.admin/show]
              ["/shows" :api.admin/shows]]]]]))

(def ^:private ui-routes
  (routes ["/" :ui/main]
          ["/about" :ui/about]
          ["/shows" :ui/shows]
          ["/photos" :ui/photos]
          ["/videos" :ui/videos]
          ["/contact" :ui/contact]
          ["/admin"
           [["" :ui.admin/main]
            ["/login" :ui.admin/login]
            ["/shows/create" :ui.admin/new-show]
            [["/shows/" :show-id] :ui.admin/show]]]))

(def ^:private s3-routes
  (routes ["/images"
           [[["/" :image] :s3/image]]]))

(def ^:private aws-routes
  (routes ["/public"
           [["/images" :aws/images]
            ["/videos" :aws/videos]
            ["/shows" :aws/shows]]]
          ["/auth"
           [["/info" :aws/info]
            ["/login" :aws/login]]]))

(defn ^:private namify [[k v]]
  [k (if (keyword? v) (name v) (str v))])

(defn match-route [path]
  (let [[uri query-string] (string/split path #"\?")]
    (-> (bidi/match-route ui-routes path)
        (assoc :uri uri)
        (maps/assoc-maybe :query-params (not-empty (into {}
                                                         (map #(string/split % #"="))
                                                         (some-> query-string (string/split #"&")))))
        (maps/update-maybe :route-params maps/update-maybe :show-id uuid))))

(defn path-for
  ([page]
   (path-for page nil))
  ([page params]
   (path-for ui-routes page params))
  ([routes page {:keys [route-params query-params]}]
   (cond-> (apply bidi/path-for routes page (mapcat namify route-params))
     (seq query-params) (str "?" (->> query-params
                                      (map (fn [[k v]]
                                             (str (name k) "=" (keywords/safe-name v))))
                                      (string/join "&"))))))

(defn api-for
  ([page]
   (api-for page nil))
  ([page params]
   (str (env/get :api-base-url) (path-for api-routes page params))))

(defn ui-for
  ([page]
   (ui-for page nil))
  ([page params]
   (str (.-origin js/location) (path-for ui-routes page params))))

(defn s3-for
  ([page]
   (s3-for page nil))
  ([page params]
   (str (env/get :aws-s3-uri) (path-for s3-routes page params))))

(defn aws-for
  ([page]
   (aws-for page nil))
  ([page params]
   (str (env/get :aws-api-uri) (path-for aws-routes page params))))

(defonce ^:private history
  (let [history (pushy/pushy (comp store/dispatch (partial conj [:router/navigate])) match-route)]
    (pushy/start! history)
    history))

(defn ^:private navigate* [history page params]
  (pushy/set-token! history (path-for page params)))

(defn ^:private nav-and-replace* [history page params]
  (pushy/replace-token! history (path-for page params)))

(defn navigate!
  ([page] (navigate* history page nil))
  ([page params]
   (navigate* history page params)
   nil))

(defn go-to! [url]
  (.assign (.-location js/window) url)
  nil)

(defn nav-and-replace!
  ([page] (nav-and-replace* history page nil))
  ([page params]
   (nav-and-replace* history page params)
   nil))

(defn login! [token]
  (.setItem js/localStorage "auth-token" token)
  (go-to! (path-for :ui.admin/main)))

(defn logout!
  ([]
   (logout! nil))
  ([params]
   (.removeItem js/localStorage "auth-token")
   (go-to! (path-for :ui.admin/login params))))
