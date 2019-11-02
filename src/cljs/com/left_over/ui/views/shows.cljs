(ns com.left-over.ui.views.shows
  (:require [com.left-over.ui.services.store.core :as store]
            [com.left-over.ui.services.store.actions :as actions]))

(defn show-list [type shows & missing-msgs]
  (if (seq shows)
    [:ul.shows
     {:classes [(name type)]}
     (for [show shows]
       ^{:key (:id show)}
       [:li.show
        (pr-str show)])]
    (into [:<>] (map (partial conj [:p])) missing-msgs)))

(defn root [_state]
  (store/dispatch actions/fetch-upcoming-shows)
  (store/dispatch actions/fetch-past-shows)
  (fn [{{[upcoming-status upcoming-result] :upcoming
         [past-status past-result] :past} :shows}]
    [:<>
     (case upcoming-status
       :init [:div.loader-container [:div.loader.large]]
       :error [:div
               [:p "something went wrong"]
               [:p "please try again later"]]
       :success
       [:div
        [show-list :upcoming upcoming-result "we don't have any upcoming shows booked" "check back soon"]])
     (case past-status
       :init [:div.loader-container [:div.loader.large]]
       :error [:div
               [:p "something went wrong"]
               [:p "please try again later"]]
       :success
       [:div
        [show-list :past past-result]])]))
