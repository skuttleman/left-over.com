(ns com.left-over.ui.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [com.left-over.ui.services.store.core :as store]
    [pushy.core :as pushy]))

(def ^:private app-routes
  [""
   [["/api"
     [["/photos" :api/photos]]]

    ["/" :ui/main]
    ["/news" :ui/news]
    ["/about" :ui/about]
    ["/shows" :ui/shows]
    ["/photos" :ui/photos]
    ["/contact" :ui/contact]
    [true :nav/not-found]]])

(defn ^:private namify [[k v]]
  [k (if (keyword? v) (name v) (str v))])

(defn match-route [path]
  (bidi/match-route app-routes path))

(defn path-for
  ([page]
   (path-for page nil))
  ([page {:keys [route-params]}]
   (apply bidi/path-for app-routes page (mapcat namify route-params))))

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
