(ns com.left-over.ui.admin
  (:require
    [cljs.core.match :refer-macros [match]]
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
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
  {:ui.admin/main (fn [_] [:div "main"])
   :ui.admin/login (fn [_] [:div "login"])
   :nav/not-found not-found})

(defn app* [{{:keys [handler]} :page :as state}]
  (let [component (handler->component handler not-found)]
    [:div.columns {:style {:min-height "100vh" :margin-top "0"}}
     [:div.column.is-variable.is-0-mobile {:style {:padding "0"}}]
     [:div.column.is-variable {:style {:height  "100vh"
                                       :padding 0}}
      [:div.rows {:style {:height         "100%"
                          :display        :flex
                          :flex-direction :column}}
       [:div.row
        [navbar/logo]]
       [:div.row
        [:div {:style {:margin "16px"}}
         [component state]]]]]
     [:div.column.is-variable.is-0-mobile {:style {:padding "0"}}]]))

(defn app []
  (let [{[status] :auth {:keys [handler]} :page :as state} (store/get-state)]
    (match [status handler]
      [:success :ui.admin/login] (nav/go-to! (nav/path-for :ui.admin/main))
      [:error (_ :guard (complement #{:ui.admin/login}))] (nav/logout!)
      :else [app* state])))

(defn ^:export mount! []
  (let [{:keys [handler query-params]} (:page (store/get-state))
        token (get query-params "token")]
    (if (and (= :ui.admin/main handler) token)
      (nav/login! token)
      (-> admin.actions/fetch-auth-info
          store/dispatch
          (v/peek (fn [_]
                    (r/render-component [app] (.getElementById js/document "app"))))))))
