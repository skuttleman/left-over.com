(ns com.left-over.ui.services.env
  (:refer-clojure :exclude [get])
  (:require-macros
    [com.left-over.ui.services.env]))

(def get
  (let [dev? (boolean (re-find #"localhost" (.-host (.-location js/window))))
        app (keyword (aget js/window "APP"))]
    (-> (com.left-over.ui.services.env/build-env)
        (assoc :dev? dev?
               :admin? (= :admin app)
               :app app))))
