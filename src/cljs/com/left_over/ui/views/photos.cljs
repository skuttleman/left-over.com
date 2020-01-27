(ns com.left-over.ui.views.photos
  (:require
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]))

(defn root* [{:keys [photos]}]
  [:div
   (if (seq photos)
     [:<>
      [:p "here are some photos"]
      [:ul.photos
       (for [{:keys [link]} photos]
         ^{:key link} [:li.photo.has-text-centered [:img {:src link}]])]]
     [:<>
      [:p "we don't have any photos to share with you right now"]
      [:p "check back soon"]])])

(defn root [_state]
  (store/dispatch actions/fetch-photos)
  (partial components/with-status #{:photos} root*))
