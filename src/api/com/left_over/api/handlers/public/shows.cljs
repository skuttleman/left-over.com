(ns com.left-over.api.handlers.public.shows
  (:require
    [com.left-over.api.core :as core]
    [com.left-over.api.services.shows :as shows]))

(def handler
  (core/handler shows/shows))

(set! (.-exports js/module) #js {:handler handler})
