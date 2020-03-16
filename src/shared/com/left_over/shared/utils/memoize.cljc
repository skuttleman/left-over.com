(ns com.left-over.shared.utils.memoize
  (:require
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])])
  #?(:clj (:import
            (java.util Date))))

(defn ^:private now-ms []
  (.getTime #?(:clj  (Date.)
               :cljs (js/Date.))))

(defn memo [f ttl max-ttl]
  (let [cache (atom {:fetched-at 0})]
    (fn [arg]
      (let [{:keys [data fetched-at]} @cache
            now (now-ms)
            promise (when (or (empty? data) (> (- now fetched-at) ttl))
                      (swap! cache assoc :fetched-at now)
                      (v/peek (f arg)
                              (fn [data] (swap! cache assoc :data data))
                              (fn [_] (swap! cache assoc :fetched-at fetched-at))))]
        (if (and promise (> (- now fetched-at) max-ttl))
          promise
          (v/resolve data))))))
