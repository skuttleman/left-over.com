(ns com.left-over.shared.utils.numbers
  (:refer-clojure :exclude [number?])
  (:require
    [com.left-over.shared.utils.strings :as strings]))

(defn nan? [value]
  (js/isNaN value))

(defn number? [value]
  (and (clojure.core/number? value) (not (nan? value))))

(defn parse-int [value]
  (js/parseInt value))

(defn parse-int! [value]
  (let [result (parse-int value)]
    (assert (not (nan? result)) (strings/format "Integer could not be parsed from: %s" value))
    result))
