(ns com.left-over.api.handlers.pub.videos
  (:require
    [com.left-over.api.core :as core]
    [com.left-over.api.services.dropbox :as dropbox]))

(def handler
  (core/with-event dropbox/fetch-videos))

(set! (.-exports js/module) #js {:handler handler})
