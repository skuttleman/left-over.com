(ns com.left-over.common.utils.maps)

(defn select-renamed-keys [m key-map]
  (reduce (fn [result [old-key new-key]]
            (if-let [[_ val] (find m old-key)]
              (assoc result new-key val)
              result))
          {}
          key-map))
