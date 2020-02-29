(ns com.left-over.ui.admin
  (:require
    [cljs.core.match :refer-macros [match]]
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.auth :as auth]
    [com.left-over.ui.admin.views.main :as admin.main]
    [com.left-over.ui.admin.views.modal :as modal]
    [com.left-over.ui.admin.views.shows :as admin.shows]
    [com.left-over.ui.admin.views.toasts :as toast]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.navbar :as navbar]
    [reagent.core :as r]))

(defn not-found [_]
  [:div
   [:p "page not found"]
   [:p
    "go "
    [:a {:href (nav/path-for :ui.admin/main)} "home"]]])

(def ^:private handler->component
  {:ui.admin/main     admin.main/root
   :ui.admin/login    auth/login
   :nav/not-found     not-found
   :ui.admin/new-show admin.shows/root
   :ui.admin/show     admin.shows/root})

(defn app* [{{:keys [query-params handler] :as page} :page}]
  (when-let [toast-msg-id (get query-params "toast-msg-id")]
    (store/dispatch (admin.actions/toast-by-id! (keyword toast-msg-id) page))
    (nav/nav-and-replace! handler (update page :query-params dissoc "toast-msg-id")))
  (fn [{{:keys [handler]} :page :as state}]
    (let [component (handler->component handler not-found)]
      [:div {:style {:min-height "100vh"
                              :max-width  "100vw"
                              :margin-top "0"}}
       [:div.is-variable {:style {:height  "100vh"
                                          :padding 0}}
        [:div.rows {:style {:height         "100%"
                            :display        :flex
                            :flex-direction :column}}
         [:div.row
          [navbar/logo true]]
         [:div.row {:style {:padding "8px"}}
          [:div {:style {:width "100%"}}
           [component state]]]]]
       [toast/toasts (:toasts state)]
       [modal/modal (:modal state)]])))

(defn app []
  (let [{[status] :auth {:keys [handler query-params]} :page :as state} (store/get-state)]
    (match [status handler]
      [:success :ui.admin/login] (nav/go-to! (nav/path-for :ui.admin/main))
      [:error (_ :guard (complement #{:ui.admin/login}))] (nav/logout! {:query-params query-params})
      :else [app* state])))

(defn ^:export mount! []
  (let [{{token "token"} :query-params :keys [handler]} (:page (store/get-state))]
    (if (and (= :ui.admin/main handler) token)
      (nav/login! token)
      (-> admin.actions/fetch-auth-info
          store/dispatch
          (v/peek (fn [_]
                    (r/render-component [app] (.getElementById js/document "app"))))))))
