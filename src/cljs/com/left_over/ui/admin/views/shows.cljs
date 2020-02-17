(ns com.left-over.ui.admin.views.shows
  (:require
    [com.left-over.common.utils.dates :as dates]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]
    [com.left-over.ui.views.dropdown :as dropdown]
    [com.ben-allred.formation.core :as f]
    [com.left-over.ui.services.forms.core :as forms]
    [com.ben-allred.vow.core :as v]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.common.utils.strings :as strings]
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
  [:span (if item
           (:name item)
           "Select…")])

(defn show-form [form-id {:keys [forms locations search]}]
  (when-let [form (get forms form-id)]
    (let [locations (map (juxt :id identity) locations)
          locations-by-id (into {} locations)
          location-options (filter (comp (partial re-find (re-pattern (str "(?i)" search))) str :name second) locations)]
      [fields/form {:on-submit (fn [{show-id :id :as show}]
                                 (-> (if show-id
                                       (admin.actions/update-show show-id show)
                                       (admin.actions/create-show show))
                                     store/dispatch
                                     (admin.actions/act-or-toast (admin.actions/navigate :ui.admin/main))
                                     store/dispatch))
                    :form      form}
       [fields/input (-> {:label "Name"}
                         (forms/with-attrs form [:name]))]
       [fields/input (-> {:label "When?"
                          :type  "datetime-local"}
                         (forms/with-attrs form [:date-time] model->view view->model))]
       [dropdown/dropdown (-> {:label           "Where?"
                               :options         location-options
                               :options-by-id   locations-by-id
                               :on-search       (comp store/dispatch (partial conj [:search/set]))
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

(defn edit-show [{:keys [show]}]
  (let [form-id (-> show
                    (select-keys #{:hidden? :location-id :name :date-time :website :id})
                    (admin.actions/create-form validator)
                    store/dispatch)]
    (partial show-form form-id)))

(defn root [{:keys [page]}]
  (let [show-id (get-in page [:route-params :show-id])]
    (store/dispatch (admin.actions/fetch-show show-id))
    (store/dispatch admin.actions/fetch-locations)
    (partial components/with-status (cond-> #{:locations} show-id (conj :show)) edit-show)))
