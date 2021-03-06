(ns com.left-over.ui.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.left-over.ui.services.env :as env]
    [com.left-over.shared.utils.keywords :as keywords]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.ui.services.store.core :as store]
    [pushy.core :as pushy]))

(def ^:const ^:private uuid-re
  #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

(defn ^:private routes [routes]
  ["" (conj routes [true :nav/not-found])])

(def ^:private ui-routes
  (routes [["/" :ui/main]
           ["/about" :ui/about]
           ["/shows" :ui/shows]
           ["/photos" :ui/photos]
           ["/music" :ui/music]
           ["/videos" :ui/videos]
           ["/contact" :ui/contact]
           ["/admin" [["" :ui.admin/main]
                      ["/login" :ui.admin/login]
                      ["/shows/create" :ui.admin/new-show]
                      ["/calendar" :ui.admin/calendar]
                      [["/shows/" [uuid-re :show-id]] :ui.admin/show]]]]))

(def ^:private s3-routes
  (routes [["/images"
            [[["/" :image] :s3/image]]]]))

(def ^:private aws-routes
  (routes [["/public" [["/images" :aws/images]
                       ["/videos" :aws/videos]
                       ["/shows" :aws/shows]
                       ["/songs" :aws/songs]]]
           ["/auth" [["/info" :aws/info]
                     ["/login" :aws/login]]]
           ["/admin" [["/shows" :aws.admin/shows]
                      ["/locations" :aws.admin/locations]
                      [["/shows/" :show-id] :aws.admin/show]
                      [["/locations/" :location-id] :aws.admin/location]
                      ["/calendar" {"/merge" :aws.admin/calendar.merge}]]]]))

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
