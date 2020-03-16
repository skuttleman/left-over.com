(ns com.left-over.shared.utils.edn
  (:require
    [#?(:clj clojure.edn :cljs cljs.tools.reader.edn) :as e]
    #?(:cljs [cljs.tagged-literals :as tags])))

(defn stringify [payload]
  (pr-str payload))

(defn parse [s]
  (if (string? s)
    (e/read-string #?(:cljs {:readers tags/*cljs-data-readers*}) s)
    (try (e/read #?(:cljs {:readers tags/*cljs-data-readers*}) s)
         (catch #?(:clj Throwable :default :default) _
           nil))))
