(ns com.left-over.shared.utils.logging
  (:require
    [taoensso.timbre :as timbre]))

(defmacro debug [& args]
  `(timbre/debug ~@args))

(defmacro info [& args]
  `(timbre/info ~@args))

(defmacro warn [& args]
  `(timbre/warn ~@args))

(defmacro error [& args]
  `(timbre/error ~@args))

(defmacro spy [x]
  `(timbre/spy :info ~x))

(defn spy-on* [x]
  (eval `(spy ~x)))

(defmacro spy-on [f]
  `(fn [& args#]
     (spy-on* (cons ~f args#))))

(defmacro spy-tap [tap-f x]
  `(let [result# ~x]
     (spy (~tap-f result#))
     result#))
