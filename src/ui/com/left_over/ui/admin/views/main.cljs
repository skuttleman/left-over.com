(ns com.left-over.ui.admin.views.main
  (:require
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.auth :as auth]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]
    [com.left-over.ui.views.shows :as shows]))


(defn root* [{:keys [shows]}]
  [:div
   [:div.row.full.space-between
    [:a.link {:href (nav/path-for :ui.admin/new-show)} "Create a show"]
    [auth/logout]]
   [shows/show-list :admin (sort-by :date-time shows) "You have no shows." "Why not create one?"]])

(defn root [_state]
  (store/dispatch admin.actions/fetch-shows)
  (partial components/with-status #{:shows} root*))
