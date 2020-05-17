(ns com.left-over.shared.utils.uri
  (:require
    [clojure.string :as string]
    [lambdaisland.uri :as uri*]
    [com.left-over.shared.utils.maps :as maps]))

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

(defn split-query [query-string]
  (when (seq query-string)
    (into {}
          (map (fn [pair]
                 (let [[k v] (string/split pair #"=")]
                   [(keyword k) (if (nil? v) true (url-decode v))])))
          (string/split query-string #"&"))))

(defn parse [uri]
  (-> uri
      uri*/parse
      (maps/update-maybe :query split-query)))

(defn stringify [uri]
  (-> uri
      (update :query form-url-encode)
      str))
