(ns com.left-over.common.utils.json
  #?(:clj
     (:require
       [jsonista.core :as j])))

(def ^:private mapper
  #?(:clj (j/object-mapper {:decode-key-fn keyword})))

(defn stringify [payload]
  #?(:clj  (j/write-value-as-string payload)
     :cljs (js/JSON.stringify (clj->js payload))))

(defn parse [s]
  #?(:clj  (j/read-value s mapper)
     :cljs (js->clj (js/JSON.parse s) :keywordize-keys true)))
