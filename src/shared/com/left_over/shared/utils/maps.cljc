(ns com.left-over.shared.utils.maps)

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

(defn assoc-maybe [m & kvs]
  (loop [m m [k v :as kvs] kvs]
    (cond
      (empty? kvs) m
      (some? v) (recur (assoc m k v) (nnext kvs))
      :else (recur m (nnext kvs)))))

(defn map-kv [key-fn val-fn m]
  (into {} (map (fn [[k v]] [(key-fn k) (val-fn v)])) m))
