(ns com.left-over.shared.utils.strings
  (:refer-clojure :exclude [format])
  (:require
    [clojure.string :as string]
    [goog.string :as gstring]
    goog.string.format))

(def format
  gstring/format)

(defn trim-to-nil [s]
  (when s
    (not-empty (string/trim s))))
