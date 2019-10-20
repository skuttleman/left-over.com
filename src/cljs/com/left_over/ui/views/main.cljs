(ns com.left-over.ui.views.main
  (:require [com.left-over.ui.services.navigation :as nav]))

(defn root [_]
  [:div.has-text-centered
   [:a {:href (nav/path-for :ui/home)} "enter"]])
