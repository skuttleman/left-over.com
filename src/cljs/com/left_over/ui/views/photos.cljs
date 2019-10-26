(ns com.left-over.ui.views.photos
  (:require
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.store.core :as store]))

(defn root [_]
  (store/dispatch actions/fetch-photos)
  (fn [{[status result] :photos}]
    (case status
      :init [:div "loading"]
      :error [:div
              [:p "something went wrong"]
              [:p "please try again later"]]
      :success
      [:div
       (if (seq result)
         [:<>
          [:p "here are some photos"]
          [:ul.photos
           (for [{:keys [link]} result]
             ^{:key link} [:li.photo.has-text-centered [:img {:src link}]])]]
         [:<>
          [:p "we don't have any photos to share with you right now"]
          [:p "check back soon"]])])))
