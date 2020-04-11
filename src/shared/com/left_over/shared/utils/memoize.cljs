(ns com.left-over.shared.utils.memoize
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.shared.utils.dates :as dates]))

(defn memo [f ttl max-ttl]
  (let [cache (atom {:fetched-at 0})]
    (fn [arg]
      (let [{:keys [data fetched-at]} @cache
            now (dates/inst->ms (dates/now))
            promise (when (or (empty? data) (> (- now fetched-at) ttl))
                      (swap! cache assoc :fetched-at now)
                      (v/peek (f arg)
                              (fn [data] (swap! cache assoc :data data))
                              (fn [_] (swap! cache assoc :fetched-at fetched-at))))]
        (if (and promise (> (- now fetched-at) max-ttl))
          promise
          (v/resolve data))))))
