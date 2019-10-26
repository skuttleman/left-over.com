(ns com.left-over.common.utils.json
  #?(:clj
     (:require
       [jsonista.core :as j])))

(defn stringify [payload]
  #?(:clj  (j/write-value-as-string payload)
     :cljs (js/JSON.stringify payload)))

(def mapper
  #?(:clj (j/object-mapper {:decode-key-fn keyword})))

(defn parse [s]
  #?(:clj  (j/read-value s mapper)
     :cljs (js/JSON.parse s)))
