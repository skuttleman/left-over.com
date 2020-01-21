(ns com.left-over.common.services.env
  (:refer-clojure :exclude [get])
  #?(:clj
     (:require
       [environ.core :as env*])))

(defn get
  ([k]
   (get k nil))
  ([k default]
   (let [env #?(:clj     env*/env
                :cljs    (let [dev? (boolean (re-find #"localhost" (.-host (.-location js/window))))]
                           {:dev?         dev?
                            :api-base-url (or (.-API_HOST js/window)
                                              "http://localhost:3000")})
                :default nil)]
     #?(:cljs (println (.-API_HOST js/window)))
     (clojure.core/get env k default))))
