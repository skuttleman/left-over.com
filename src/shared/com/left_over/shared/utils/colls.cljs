(ns com.left-over.shared.utils.colls)

(defn force-sequential [x]
  (cond-> x
    (not (and (coll? x) (sequential? x))) vector))

(defn force-vector [x]
  (if (vector? x)
    x
    [x]))

(defn only! [[item & more]]
  (when (seq more)
    (throw (ex-info "collection has more than one item" {})))
  item)

(defn organize [pred coll]
  [(filter pred coll) (remove pred coll)])

(defn collect-related [grouper leader? joiner coll]
  (->> coll
       (group-by grouper)
       (keep (fn [[group coll]]
               (let [{[leader] true followers false} (group-by (comp boolean leader?) coll)]
                 (when leader
                   (joiner leader group followers)))))))
