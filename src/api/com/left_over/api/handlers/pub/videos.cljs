(ns com.left-over.api.handlers.pub.videos
  (:require
    [com.left-over.api.core :as core]
    [com.left-over.api.services.dropbox :as dropbox]))

(def handler
  (core/handler dropbox/fetch-images))

(set! (.-exports js/module) #js {:handler handler})
