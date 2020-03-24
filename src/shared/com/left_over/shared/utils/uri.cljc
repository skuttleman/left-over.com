(ns com.left-over.shared.utils.uri
  (:require
    [clojure.string :as string]
    [lambdaisland.uri :as uri*])
  #?(:clj (:import
            (java.net URLDecoder URLEncoder))))

(defn url-encode [arg]
  #?(:clj  (URLEncoder/encode (str arg) "UTF-8")
     :cljs (js/encodeURIComponent (str arg))))

(defn url-decode [arg]
  (let [arg (string/replace (str arg) #"\+" " ")]
    #?(:clj  (URLDecoder/decode arg "UTF-8")
       :cljs (js/decodeURIComponent arg))))

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
