(ns com.left-over.shared.utils.uri
  (:require
    [clojure.string :as string]
    [lambdaisland.uri :as uri*]))

(defn url-encode [arg]
  (js/encodeURIComponent (str arg)))

(defn url-decode [arg]
  (-> arg
      str
      (string/replace #"\+" " ")
      (js/decodeURIComponent)))

(defn form-url-encode [arg]
  (string/join \& (map (fn [[k v]]
                         (if (vector? v)
                           (form-url-encode (map (fn [v] [k v]) v))
                           (str (url-encode (name k))
                                \=
                                (url-encode v))))
                       arg)))

(def ^{:arglists '([uri])} parse
  uri*/parse)

(defn stringify [uri]
  (-> uri
      (update :query form-url-encode)
      str))
