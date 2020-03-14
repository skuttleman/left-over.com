(ns com.left-over.common.utils.memoize
  (:require
    [com.ben-allred.vow.core :as v])
  #?(:clj (:import
            (java.util Date))))

(defn ^:private now-ms []
  (.getTime #?(:clj  (Date.)
               :cljs (js/Date.))))

(defn memo [f ttl max-ttl]
  (let [cache (atom {:fetched-at 0})]
    (fn []
      (let [{:keys [data fetched-at]} @cache
            now (now-ms)
            promise (when (or (empty? data) (> (- now fetched-at) ttl))
                      (swap! cache assoc :fetched-at now)
                      (v/peek (f)
                              (fn [data] (swap! cache assoc :data data))
                              (fn [_] (swap! cache assoc :fetched-at fetched-at))))]
        (if (and promise (> (- now fetched-at) max-ttl))
          promise
          (v/resolve data))))))
