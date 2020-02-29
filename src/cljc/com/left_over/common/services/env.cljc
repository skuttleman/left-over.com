(ns com.left-over.common.services.env
  (:refer-clojure :exclude [get])
  #?(:clj
     (:require
       [environ.core :as env*])))

(def get
  #?(:clj  env*/env
     :cljs (let [dev? (boolean (re-find #"localhost" (.-host (.-location js/window))))
                 app (keyword (aget js/window "APP"))]
             {:dev?         dev?
              :api-base-url (or (aget js/window "API_HOST")
                                "http://localhost:3000")
              :admin?       (= :admin app)
              :app          app})))
