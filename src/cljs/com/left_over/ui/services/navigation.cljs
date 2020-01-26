(ns com.left-over.ui.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.left-over.common.services.env :as env]
    [com.left-over.common.utils.keywords :as keywords]
    [com.left-over.common.utils.maps :as maps]
    [com.left-over.ui.services.store.core :as store]
    [pushy.core :as pushy]))

(def ^:private app-routes
  [""
   [["/api"
     [["/photos" :api/photos]
      [["/images/" :image] :api/image]
      ["/shows" :api/shows]]]

    ["/auth"
     [["/info" :auth/info]
      ["/login" :auth/login]
      ["/logout" :auth/logout]]]

    ["/" :ui/main]
    ["/about" :ui/about]
    ["/shows" :ui/shows]
    ["/photos" :ui/photos]
    ["/contact" :ui/contact]

    ["/admin"
     [["" :ui.admin/main]
      ["/login" :ui.admin/login]]]

    [true :nav/not-found]]])

(defn ^:private namify [[k v]]
  [k (if (keyword? v) (name v) (str v))])

(defn match-route [path]
  (let [[uri query-string] (string/split path #"\?")]
    (-> (bidi/match-route app-routes path)
        (assoc :uri uri)
        (maps/assoc-maybe :query-params (not-empty (into {}
                                                         (map #(string/split % #"="))
                                                         (some-> query-string (string/split #"&"))))))))

(defn path-for
  ([page]
   (path-for page nil))
  ([page {:keys [route-params query-params]}]
   (cond-> (apply bidi/path-for app-routes page (mapcat namify route-params))
     (seq query-params) (str "?" (->> query-params
                                      (map (fn [[k v]]
                                             (str (name k) "=" (keywords/safe-name v))))
                                      (string/join "&"))))))

(defn api-for
  ([page]
   (api-for page nil))
  ([page params]
   (str (env/get :api-base-url) (path-for page params))))

(defn ui-for
  ([page]
   (ui-for page nil))
  ([page params]
   (str (.-origin js/location) (path-for page params))))

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
