(ns com.left-over.common.services.env
  (:refer-clojure :exclude [get])
  (:require
    [clojure.java.io :as io]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.keywords :as keywords]))

(def get
  (let [f (io/file ".lein-env")]
    (into {}
          (map (juxt (comp keywords/keyword key) val))
          (concat (System/getenv)
                  (when (.exists f)
                    (edn/parse (slurp f)))))))

(defmacro build-env []
  get)
