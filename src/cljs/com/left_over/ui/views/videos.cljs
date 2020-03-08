(ns com.left-over.ui.views.videos
  (:require [com.left-over.ui.services.store.actions :as actions]
            [com.left-over.ui.services.store.core :as store]
            [com.left-over.ui.views.components :as components]))


(defn root* [{:keys [videos]}]
  [:div
   (if (seq videos)
     [:ul.videos
      (for [{:keys [link metadata]} videos]
        ^{:key link} [:li.video
                      [:video {:controls true :width "100%" :height "100%"}
                       [:source {:src link :type (:mime-type metadata)}]
                       "Sorry, your browser doesn't support embedded videos."]])]
     [:<>
      [:p "We don't have any videos to share with you right now."]
      [:p "Check back soon."]])])

(defn root [_state]
  (store/dispatch actions/fetch-videos)
  (partial components/with-status #{:videos} root*))
