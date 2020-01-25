(ns com.left-over.common.utils.strings
  (:refer-clojure :exclude [format])
  #?(:cljs
     (:require
       [goog.string :as gstring]
       [goog.string.format])))

(def format
  #?(:clj  clojure.core/format
     :cljs gstring/format))
