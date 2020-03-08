(ns com.left-over.common.services.env
  (:refer-clojure :exclude [get])
  (:require
    [com.left-over.common.utils.keywords :as keywords]
    [environ.core :as env*]))

(def ^:private js-env-vars [:api-base-url
                            :aws-api-key
                            :aws-api-uri
                            :aws-s3-uri])

(defmacro env []
  (into {} (map (juxt keywords/keyword env*/env)) js-env-vars))

(def get env*/env)
