(ns com.left-over.api.handlers.pub.songs
  (:require
    [com.left-over.api.core :as core]
    [com.left-over.api.services.songs :as songs]))

(def handler
  (core/with-event songs/songs))

(set! (.-exports js/module) #js {:handler handler})
