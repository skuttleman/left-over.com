(ns com.left-over.ui.admin.views.auth
  (:require
    [com.ben-allred.formation.core :as f]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]))

(def validator
  (f/validator {:email    (f/required "You must enter your email")
                :password (f/required "You must enter your password")}))

(defn login [_state]
  (let [form-id (store/dispatch (admin.actions/create-form nil validator))]
    (fn [{{form form-id} :forms}]
      (when form
        [fields/form {:on-submit      (fn [params]
                                        (let [qp (->> (nav/ui-for :ui.admin/main)
                                                      js/encodeURIComponent
                                                      (assoc params :redirect-uri))]
                                          (nav/go-to! (nav/api-for :auth/login {:query-params qp}))))
                      :form           form
                      :button-content "Login"}
         [fields/input (-> {:type :text :auto-focus true :label "email"}
                           (forms/with-attrs form [:email]))]
         [fields/input (-> {:type :password :label "password"}
                           (forms/with-attrs form [:password]))]]))))

(defn logout []
  [:button.button.is-link
   {:on-click (fn [_]
                (nav/logout!))}
   "Logout"])
