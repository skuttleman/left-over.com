(ns com.left-over.shared.utils.maps)

(defn select-renamed-keys [m key-map]
  (reduce (fn [result [old-key new-key]]
            (if-let [[_ val] (find m old-key)]
              (assoc result new-key val)
              result))
          {}
          key-map))

(defn default [m k else]
  (update m k #(or % else)))

(defn update-maybe [m k f & f-args]
  (if (contains? m k)
    (apply update m k f f-args)
    m))

(defn update-in-maybe [m [k & ks] f & f-args]
  (if (seq ks)
    (apply update-maybe m k update-in-maybe ks f f-args)
    (apply update-maybe m k f f-args)))

(defn assoc-maybe [m & kvs]
  (loop [m m [k v :as kvs] kvs]
    (cond
      (empty? kvs) m
      (some? v) (recur (assoc m k v) (nnext kvs))
      :else (recur m (nnext kvs)))))

(defn map-kv [key-fn val-fn m]
  (into {} (map (fn [[k v]] [(key-fn k) (val-fn v)])) m))

(defn supermap? [m1 m2]
  (and (>= (count m1) (count m2))
       (every? (fn [[k v]]
                 (= [k v] (find m1 k)))
               m2)))

(defn submap? [m1 m2]
  (supermap? m2 m1))
