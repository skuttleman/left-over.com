(ns com.left-over.ui.admin.views.auth
  (:require
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.services.navigation :as nav]))

(defn login [_state]
  (let [form-id (store/dispatch (admin.actions/create-form nil))]
    (fn [{{form form-id} :forms}]
      (when form
        [fields/form {:on-submit (fn [_]
                                   (let [qp (->> (nav/ui-for :ui.admin/main)
                                                 js/encodeURIComponent
                                                 (assoc @form :redirect-uri))]
                                     (nav/go-to! (nav/api-for :auth/login {:query-params qp}))))}
         [:fieldset.field {:style {:outline :none :padding 0 :border :none}}
          [:label.label "email"]
          [fields/input (-> {:type :text :auto-focus true}
                            (forms/with-attrs form [:email] identity identity))]]
         [:fieldset.field {:style {:outline :none :padding 0 :border :none}}
          [:label.label "password"]
          [fields/input (-> {:type :password}
                            (forms/with-attrs form [:password] identity identity))]]
         [:button.button.is-link {:type :submit} "Login"]]))))

(defn logout []
  [:button.button.is-link
   {:on-click (fn [_]
                (nav/logout!))}
   "Logout"])
