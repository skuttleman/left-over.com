(ns com.left-over.common.utils.logging
  (:require
    [clojure.pprint :as pp]
    [taoensso.timbre :as timbre :include-macros true])
  (:require-macros [com.left-over.common.utils.logging]))

(defn pprint [x]
  [:pre
   (with-out-str (pp/pprint x))])
