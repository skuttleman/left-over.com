(ns com.left-over.ui.admin.views.locations
  (:require
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.services.store.core :as store]))

(def states
  [["AL" "Alabama"]
   ["AK" "Alaska"]
   ["AZ" "Arizona"]
   ["AR" "Arkansas"]
   ["CA" "California"]
   ["CO" "Colorado"]
   ["CT" "Connecticut"]
   ["DE" "Delaware"]
   ["FL" "Florida"]
   ["GA" "Georgia"]
   ["HI" "Hawaii"]
   ["ID" "Idaho"]
   ["IL" "Illinois"]
   ["IN" "Indiana"]
   ["IA" "Iowa"]
   ["KS" "Kansas"]
   ["KY" "Kentucky"]
   ["LA" "Louisiana"]
   ["ME" "Maine"]
   ["MD" "Maryland"]
   ["MA" "Massachusetts"]
   ["MI" "Michigan"]
   ["MN" "Minnesota"]
   ["MS" "Mississippi"]
   ["MO" "Missouri"]
   ["MT" "Montana"]
   ["NE" "Nebraska"]
   ["NV" "Nevada"]
   ["NH" "New Hampshire"]
   ["NJ" "New Jersey"]
   ["NM" "New Mexico"]
   ["NY" "New York"]
   ["NC" "North Carolina"]
   ["ND" "North Dakota"]
   ["OH" "Ohio"]
   ["OK" "Oklahoma"]
   ["OR" "Oregon"]
   ["PA" "Pennsylvania"]
   ["RI" "Rhode Island"]
   ["SC" "South Carolina"]
   ["SD" "South Dakota"]
   ["TN" "Tennessee"]
   ["TX" "Texas"]
   ["UT" "Utah"]
   ["VT" "Vermont"]
   ["VA" "Virginia"]
   ["WA" "Washington"]
   ["WV" "West Virginia"]
   ["WI" "Wisconsin"]
   ["WY" "Wyoming"]])

(def validator
  (f/validator {:name  (f/required "You must have a name for the location")
                :city  (f/required "You must have a city for the location")
                :state [(f/required "You must have a state for the location")
                        (f/pred (set (map first states)) "Must be a valid state")]}))

(defn ^:private save-location [location-id show-form]
  (fn [location]
    (store/dispatch (admin.actions/save-location location-id show-form location))))

(defn ^:private cancel-form [_]
  (store/dispatch admin.actions/hide-modal))

(defn location-form [show-form location]
  (let [form-id (-> location
                    (select-keys #{:name :city :state :website})
                    (update :state #(or % "MD"))
                    (admin.actions/create-form validator)
                    store/dispatch)
        on-submit (save-location (:id location) show-form)]
    (fn [_show-form _location]
      (let [{{form form-id} :forms} (store/get-state)]
        (when form
          [fields/form {:on-submit      on-submit
                        :on-cancel      cancel-form
                        :form           form
                        :button-content "Save"}
           [fields/input (-> {:label "Name" :auto-focus true}
                             (forms/with-attrs form [:name]))]
           [fields/input (-> {:label "City"}
                             (forms/with-attrs form [:city]))]
           [fields/select
            (-> {:label "State"}
                (forms/with-attrs form [:state]))
            states]
           [fields/input (-> {:label "Website URL"}
                             (forms/with-attrs form [:website]))]])))))
