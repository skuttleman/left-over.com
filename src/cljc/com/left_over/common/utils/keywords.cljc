(ns com.left-over.common.utils.keywords
  (:refer-clojure :exclude [keyword replace str])
  (:require [clojure.string :as string]))

(defn str [v]
  (if (keyword? v)
    (subs (clojure.core/str v) 1)
    v))

(defn safe-name [v]
  (if (keyword? v)
    (name v)
    v))

(defn keyword [v]
  (clojure.core/keyword (cond-> v
                          (string? v) (-> string/lower-case
                                          (string/replace #"_" "-")))))
