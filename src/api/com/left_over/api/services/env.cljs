(ns com.left-over.api.services.env
  (:refer-clojure :exclude [get])
  (:require-macros
    [com.left-over.api.services.env]))

(def get
  (let [env (com.left-over.api.services.env/env)]
    (assoc env :dev? (= (:environment env "development") "development"))))
