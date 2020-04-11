(ns com.left-over.shared.utils.edn
  (:require
    [#?(:clj clojure.edn :cljs cljs.tools.reader.edn) :as e]
    #?@(:clj  [[clojure.java.io :as io]]
        :cljs [[cljs.tagged-literals :as tags]
               [cljs.tools.reader.reader-types :as rt]]))
  #?(:clj
     (:import (java.io PushbackReader))))

(defn stringify [payload]
  (pr-str payload))

(defn parse [s]
  (cond
    #?@(:cljs [(js/Buffer.isBuffer s) (e/read-string {:readers tags/*cljs-data-readers*} (str s))])
    (string? s) (e/read-string #?(:cljs {:readers tags/*cljs-data-readers*}) s)
    #?@(:clj [(instance? PushbackReader s) (e/read s)])
    :else (try (e/read #?(:cljs {:readers tags/*cljs-data-readers*}) (-> s #?@(:clj [io/reader PushbackReader.])))
               (catch #?(:clj Throwable :default :default) _
                 nil))))
