(ns com.left-over.ui.views.shows
  (:require
    [com.left-over.common.utils.dates :as dates]
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]))

(defn show-list [type shows & missing-msgs]
  (if (seq shows)
    [:ul.shows
     {:classes [(name type)]}
     (for [show shows]
       ^{:key (:id show)}
       [:li.show.tile.is-parent
        [:div.tile.box.is-child
         [:p [:em [:strong (:name show)]]]
         [:p (get-in show [:location :name])]
         (when-let [dt (:date-time show)]
           [:time
            {:dateTime (dates/format dt :date/system)}
            (dates/relative dt)
            " @ "
            (dates/format dt :time/view)])]])]
    (into [:<>] (map (partial conj [:p])) missing-msgs)))

(defn root* [{:keys [shows]}]
  (let [[past future] (->> shows
                           (sort-by :date-time)
                           (split-with (comp pos?
                                             (partial compare (dates/->inst (dates/now)))
                                             :date-time)))]
    [:<>
     [:div
      [:p [:strong "Upcoming Shows"]]
      [show-list :upcoming future "we don't have any upcoming shows booked" "check back soon"]]
     [:div
      [:p [:strong "Past Shows"]]
      [show-list :past (reverse past) "Unable to fetch past shows"]]]))

(defn root [_state]
  (store/dispatch actions/fetch-shows)
  (partial components/with-status #{:shows} root*))
