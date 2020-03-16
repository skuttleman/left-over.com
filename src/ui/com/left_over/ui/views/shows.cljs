(ns com.left-over.ui.views.shows
  (:require
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]))

(defn ^:private delete-show [show-id]
  (fn [_]
    (store/dispatch (admin.actions/show-modal "Are you sure you want to delete this show?"
                                              "Delete Show"
                                              [:button.button.is-danger
                                               {:on-click (fn [_]
                                                            (-> show-id
                                                                admin.actions/delete-show
                                                                store/dispatch
                                                                (admin.actions/act-or-toast admin.actions/fetch-shows)
                                                                store/dispatch))}
                                               "Delete"]
                                              [:button.button "Cancel"]))))

(defn show-list [type shows & missing-msgs]
  (if (seq shows)
    [:ul.shows
     {:class [(name type)]}
     (for [{show-id :id :keys [location] :as show} shows]
       ^{:key show-id}
       [:li.show.tile.is-parent
        [:div.tile.box.is-child
         [:p
          [(if (:website show)
             :a
             :span)
           {:target :_blank :href (:website show)}
           [:em [:strong (:name show)]]]
          (when (:hidden? show)
            [:em {:style {:color :gray}} " (not visible on website)"])]
         [:p [(if (:website location)
                :a
                :span)
              {:target :_blank :href (:website location)}
              (:name location) " - " (:city location) ", " (:state location)]]
         (when-let [dt (:date-time show)]
           [:time
            {:dateTime (dates/format dt :date/system)}
            (dates/relative dt)
            " @ "
            (dates/format dt :time/view)])
         (when (= :admin type)
           [:p.row.space-between
            [:a {:href (nav/path-for :ui.admin/show {:route-params {:show-id show-id}})} "Edit"]
            [:button.button.is-link.is-danger
             {:on-click (delete-show show-id)}
             "Delete"]])]])]
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
      [show-list :upcoming future "We don't have any upcoming shows booked." "Check back soon."]]
     [:div
      [:p [:strong "Past Shows"]]
      [show-list :past (reverse past) "No past shows"]]]))

(defn root [_state]
  (store/dispatch actions/fetch-shows)
  (partial components/with-status #{:shows} root*))
