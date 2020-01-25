(ns com.left-over.common.utils.keywords
  (:refer-clojure :exclude [replace str]))

(defn str [v]
  (if (keyword? v)
    (subs (clojure.core/str v) 1)
    v))

(defn safe-name [v]
  (if (keyword? v)
    (name v)
    v))
