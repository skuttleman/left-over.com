(ns com.left-over.common.utils.colls)

(defn force-sequential [x]
  (cond-> x
    (not (and (coll? x) (sequential? x))) vector))

(defn only! [[item & more]]
  (when (seq more)
    (throw (ex-info "collection has more than one item" {})))
  item)
