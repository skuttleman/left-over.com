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

(defn past-shows
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :shows.past/request) [:init]
     :shows.past/success [:success result]
     :shows.past/failure [:error result]
     state)))

(defn upcoming-shows
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :shows.upcoming/request) [:init]
     :shows.upcoming/success [:success result]
     :shows.upcoming/failure [:error result]
     state)))

(def reducer (cr/combine {:page   page
                          :photos photos
                          :shows  (cr/combine {:past     past-shows
                                               :upcoming upcoming-shows})}))
