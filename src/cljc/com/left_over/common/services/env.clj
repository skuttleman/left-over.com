(ns com.left-over.common.services.env
  (:refer-clojure :exclude [get])
  (:require
    [com.left-over.common.utils.keywords :as keywords]
    [environ.core :as env*]))

(def ^:private js-env-vars ["API_BASE_URL"
                            "AWS_API_KEY"
                            "DROPBOX_IMAGES_URI"])

(defmacro env []
  (into {} (map (fn [k] [(keywords/keyword k) (System/getenv k)])) js-env-vars))

(def get env*/env)
