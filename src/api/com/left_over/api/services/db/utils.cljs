(ns com.left-over.api.services.db.utils
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn assoc-coll-maps [results conn ids->query k [results-id foreign-key]]
  (-> (map results-id results)
      distinct
      (->> (ids->query conn))
      (v/then (fn [items]
                (let [grouped (group-by foreign-key items)]
                  (map (fn [result]
                         (assoc result k (get grouped (results-id result) [])))
                       results))))))
