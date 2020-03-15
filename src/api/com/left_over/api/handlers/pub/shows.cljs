(ns com.left-over.api.handlers.pub.shows
  (:require
    [com.left-over.api.core :as core]
    [com.left-over.api.services.shows :as shows]))

(def handler
  (core/with-event shows/shows))

(set! (.-exports js/module) #js {:handler handler})
