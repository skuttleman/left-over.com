(ns com.left-over.shared.utils.logging
  (:require
    [clojure.pprint :as pp]
    [taoensso.timbre :as timbre :include-macros true])
  (:require-macros [com.left-over.shared.utils.logging]))

(defn pprint [x]
  [:pre
   (with-out-str (pp/pprint x))])
