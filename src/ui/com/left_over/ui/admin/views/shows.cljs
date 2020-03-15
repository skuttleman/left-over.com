(ns com.left-over.ui.admin.views.shows
  (:require
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.common.utils.dates :as dates]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.admin.views.locations :as locations]
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]
    [com.left-over.ui.views.dropdown :as dropdown]
    [reagent.core :as r]))

(def validator
  (f/validator {:date-time   (f/required "Must select a date/time for the show")
                :name        (f/required "You must have a name for the show")
                :location-id (f/required "You must select a location")}))

(def model->view
  {:date-time #(some-> % (dates/format :datetime/local))
   :hidden?   not})

(def view->model
  {:date-time #(some-> % dates/local-dt-str->inst)
   :hidden?   not})

(defn location-item [item]
  [:span (:name item)])

(defn selected-item [{[item] :selected}]
  [:span
   {:style {:white-space :nowrap
            :overflow :hidden
            :text-overflow :ellipsis}}
   (if item
           (:name item)
           "Select…")])

(defn ^:private on-search [value]
  (store/dispatch [:search/set value]))

(defn ^:private location-form-modal [form search]
  (fn [location]
    (store/dispatch (admin.actions/show-modal [locations/location-form form (or location {:name search})]
                                              (str (if location "Edit" "Create") " Location")))))

(defn show-form [form-id {:keys [forms locations search]}]
  (when-let [form (get forms @form-id)]
    (let [locations (map (juxt :id identity) locations)
          locations-by-id (into {} locations)
          location-options (->> locations
                                (sort-by (comp :name second))
                                (filter (comp (partial re-find (re-pattern (str "(?i)" search))) str :name second)))]
      [fields/form {:on-submit (fn [{show-id :id :as show}]
                                 (-> (if show-id
                                       (admin.actions/update-show show-id show)
                                       (admin.actions/create-show show))
                                     store/dispatch
                                     (admin.actions/act-or-toast (admin.actions/navigate :ui.admin/main))
                                     store/dispatch))
                    :form      form}
       [fields/input (-> {:label "Name" :auto-focus true}
                         (forms/with-attrs form [:name]))]
       [fields/input (-> {:label "When?"
                          :type  "datetime-local"}
                         (forms/with-attrs form [:date-time] model->view view->model))]
       [dropdown/dropdown (-> {:class           ["location-selector"]
                               :label           "Where?"
                               :options         location-options
                               :options-by-id   locations-by-id
                               :on-edit         (location-form-modal form search)
                               :on-search       on-search
                               :search          search
                               :item-control    location-item
                               :display-text-fn selected-item}
                              (forms/with-attrs form [:location-id])
                              dropdown/singleable)]
       [fields/checkbox (-> {:label "Show on website?"
                             :class :large}
                            (forms/with-attrs form [:hidden?] model->view view->model))]
       [fields/input (-> {:label "Website URL"}
                         (forms/with-attrs form [:website]))]])))

(defn root [{:keys [page]}]
  (let [show-id (get-in page [:route-params :show-id])
        form-id (r/atom nil)]
    (-> (or (some-> show-id admin.actions/fetch-show store/dispatch)
            (v/resolve))
        (v/then (fn [{[_ show] :show}]
                  (-> show
                      (select-keys #{:hidden? :location-id :name :date-time :website :id})
                      (admin.actions/create-form validator)
                      store/dispatch)))
        (v/then (partial reset! form-id)))
    (store/dispatch admin.actions/fetch-locations)
    (partial components/with-status (cond-> #{:locations} show-id (conj :show)) [show-form form-id])))
