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
