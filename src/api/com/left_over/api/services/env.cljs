(ns com.left-over.api.services.env
  (:refer-clojure :exclude [get])
  (:require
    [com.left-over.shared.utils.keywords :as keywords])
  (:require-macros
    [com.left-over.api.services.env]))

(def get
  (let [env (into (com.left-over.api.services.env/build-env)
                  (map (juxt keywords/keyword #(aget js/process.env %)))
                  (js/Object.keys js/process.env))]
    (assoc env :dev? (= (:environment env "dev") "dev"))))
