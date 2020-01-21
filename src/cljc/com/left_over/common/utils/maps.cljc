(ns com.left-over.common.utils.maps)

(defn select-renamed-keys [m key-map]
  (reduce (fn [result [old-key new-key]]
            (if-let [[_ val] (find m old-key)]
              (assoc result new-key val)
              result))
          {}
          key-map))

(defn update-maybe [m k f & f-args]
  (if (contains? m k)
    (apply update m k f f-args)
    m))
