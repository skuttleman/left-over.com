(ns com.left-over.ui.services.store.reducers
  (:require
    [com.ben-allred.collaj.reducers :as cr]))

(defn page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(defn photos
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :photos/request) [:init]
     :photos/success [:success result]
     :photos/failure [:error result]
     state)))

(defn shows
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :shows/request) [:init]
     :shows/success [:success result]
     :shows/failure [:error result]
     state)))

(defn videos
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :videos/request) [:init]
     :videos/success [:success result]
     :videos/failure [:error result]
     state)))

(def reducer (cr/combine {:page   page
                          :photos photos
                          :shows  shows
                          :videos videos}))
