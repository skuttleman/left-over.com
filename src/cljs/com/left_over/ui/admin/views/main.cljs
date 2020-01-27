(ns com.left-over.ui.admin.views.main
  (:require
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.auth :as auth]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]))

(defn root* [{:keys [shows locations]}]
  [:div
   [log/pprint shows]
   [log/pprint locations]
   [auth/logout]])

(defn root [_state]
  (store/dispatch admin.actions/fetch-shows)
  (store/dispatch admin.actions/fetch-locations)
  (partial components/with-status #{:shows :locations} root*))
