(ns com.left-over.ui.services.env
  (:refer-clojure :exclude [get])
  (:require
    [clojure.java.io :as io]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.keywords :as keywords]))

(def ^:private js-env-vars #{:api-base-url
                             :aws-api-key
                             :aws-api-uri
                             :aws-s3-uri})

(defmacro env []
  (let [f (io/file ".lein-env")]
    (into {}
          (comp (map (juxt (comp keywords/keyword key) val))
                (filter (comp js-env-vars first)))
          (concat (System/getenv)
                  (when (.exists f)
                    (edn/parse (slurp f)))))))
