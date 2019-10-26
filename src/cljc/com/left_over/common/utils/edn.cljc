(ns com.left-over.common.utils.edn
  (:require
    [#?(:clj clojure.edn :cljs cljs.tools.reader.edn) :as e]))

(defn stringify [payload]
  (pr-str payload))

(defn parse [s]
  (if (string? s)
    (e/read-string s)
    (e/read s)))
