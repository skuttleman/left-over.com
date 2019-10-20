(ns com.left-over.ui.services.store.reducers
  (:require
    [com.ben-allred.collaj.reducers :as cr]))


(defn page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(def reducer (cr/combine {:page page}))
