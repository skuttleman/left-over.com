(ns com.left-over.common.utils.strings
  (:refer-clojure :exclude [format])
  (:require
    [clojure.string :as string]
    #?@(:cljs
        [[goog.string :as gstring]
         [goog.string.format]])))

(def format
  #?(:clj  clojure.core/format
     :cljs gstring/format))

(defn trim-to-nil [s]
  (when s
    (not-empty (string/trim s))))
