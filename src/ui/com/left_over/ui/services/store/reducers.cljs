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

(defn songs
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :songs/request) [:init]
     :songs/success [:success result]
     :songs/failure [:error result]
     state)))

(defn selected-song
  ([] nil)
  ([state [type data]]
   (case type
     (:router/navigate :songs/request) nil
     :songs/success {:percentage 0
                     :song       (let [album (first data)]
                                   (some-> (:songs album) first (assoc :album (dissoc album :songs))))}
     :song/select {:percentage 0
                   :song       data}
     :song/update-percentage (assoc state :percentage data)
     state)))

(defn videos
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :videos/request) [:init]
     :videos/success [:success result]
     :videos/failure [:error result]
     state)))

(def reducer (cr/combine {:page          page
                          :photos        photos
                          :shows         shows
                          :songs         songs
                          :selected-song selected-song
                          :videos        videos}))
