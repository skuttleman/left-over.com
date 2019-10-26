(ns com.left-over.common.services.env
  (:refer-clojure :exclude [get])
  #?(:clj
     (:require
       [environ.core :as env*])))

(def get
  #?(:clj  env*/env
     :cljs (let [dev? (boolean (re-find #"localhost" (.-host (.-location js/window))))]
             {:dev? dev?
              :api-base-url (if dev?
                              "http://localhost:3000"
                              "https://left-over-api.herokuapp.com")})))
