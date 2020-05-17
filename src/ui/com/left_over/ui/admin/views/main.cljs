(ns com.left-over.ui.admin.views.main
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.auth :as auth]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]
    [com.left-over.ui.views.shows :as shows]))

(defn ^:private remove-shows [show-ids]
  (fn [_]
    (store/dispatch (admin.actions/remove-shows show-ids))))

(defn ^:private confirm-list [shows show?]
  [:ul.column.spaced
   {:class [(if show? "show-events" "not-show-events")]}
   (for [{show-id :id :keys [temp-data]} shows]
     ^{:key show-id}
     [:li.card
      [:div.row.full.spaced
       [:div.column
        [:strong (:summary temp-data "Unknown")]
        (when-let [dt (or (some-> (get-in temp-data [:start :dateTime]) (dates/format :datetime/view))
                          (some-> (get-in temp-data [:start :date]) (dates/format :date/view)))]
          [:em dt])]
       [:div.column.no-grow
        {:style {:align-items :center}}
        [:a.link {:href (nav/path-for :ui.admin/show {:route-params {:show-id show-id}})} "Convert"]
        [:button.button.is-danger
         {:on-click (remove-shows [show-id])}
         "Remove"]]]])])

(defn ^:private calendar* [{:keys [calendar]}]
  (let [[shows non-shows] (->> calendar
                               (map #(maps/update-in-maybe % [:temp-data :start :date] dates/parse))
                               (colls/organize (comp :show? :temp-data)))]
    [:div
     [:div.row.full.spaced
      [:div.row.spaced
       [:a.link {:href (nav/path-for :ui.admin/new-show)} "Create a show"]
       [:span.link-divider]
       [:a.link {:href (nav/path-for :ui.admin/main)} "Manage shows"]]
      [auth/logout]]
     (if (seq calendar)
       [:div.column.spaced
        (if (seq shows)
          [:<>
           [:p "These events look like shows. Please convert or remove them."]
           [confirm-list shows true]]
          [:p "No events that look like shows"])
        (when (seq non-shows)
          [:<>
           [:p "These events do not look like shows, but you may want to double check.
                Please convert or remove them."]
           [:button.button.is-danger.row.spaced
            {:on-click (remove-shows (map :id non-shows))}
            [:span "Remove all"]
            [components/icon :arrow-down]]
           [confirm-list non-shows false]])]
       [:<>
        [:p "You have no unmerged shows."]
        [:p "Check back later."]])]))

(defn calendar [_state]
  (v/always (store/dispatch admin.actions/merge-calendar!)
    (store/dispatch admin.actions/fetch-unconfirmed))
  (partial components/with-status #{:calendar} calendar*))

(defn ^:private root* [{:keys [shows]}]
  [:div
   [:div.row.full.spaced
    [:div.row.spaced
     [:a.link {:href (nav/path-for :ui.admin/new-show)} "Create a show"]
     [:span.link-divider]
     [:a.link {:href (nav/path-for :ui.admin/calendar)} "Manage calendar events"]]
    [auth/logout]]
   [shows/show-list :admin (sort-by :date-time shows) "You have no shows." "Why not create one?"]])

(defn root [_state]
  (store/dispatch admin.actions/fetch-shows)
  (partial components/with-status #{:shows} root*))
