(ns com.left-over.api.services.env
  (:refer-clojure :exclude [get])
  (:require
    [clojure.java.io :as io]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.keywords :as keywords]))

(defmacro build-env []
  (let [f (io/file ".lein-env")]
    (into {}
          (map (juxt (comp keywords/keyword key) val))
          (concat (System/getenv)
                  (when (.exists f)
                    (edn/parse (slurp f)))))))

(def get (build-env))
