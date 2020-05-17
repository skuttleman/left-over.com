(ns com.left-over.api.services.env
  (:require
    [clojure.java.io :as io]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.keywords :as keywords]))

(defmacro build-env []
  (let [f (io/file (or (System/getenv "ENV_FILE") ""))]
    (into {}
          (map (juxt (comp keywords/keyword key) val))
          (concat (System/getenv)
                  (when (.exists f)
                    (edn/parse f))))))
