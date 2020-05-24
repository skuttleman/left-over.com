(ns com.left-over.ui.admin.views.shows
  (:require
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.left-over.ui.admin.views.auth :as auth]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.admin.views.locations :as locations]
    [com.left-over.ui.moment :as mo]
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]
    [com.left-over.ui.views.dropdown :as dropdown]
    [reagent.core :as r]))

(def validator
  (f/validator {:date-time   (f/required "You must select a date/time for the show")
                :name        (f/required "You must have a name for the show")
                :location-id (f/required "You must select a location")}))

(def model->view
  {:date-time #(some-> % (mo/format :datetime/local))
   :hidden?   not})

(def view->model
  {:date-time #(some-> % mo/parse)
   :hidden?   not})

(defn location-item [item]
  [:span (:name item)])

(defn selected-item [{[item] :selected}]
  [:span
   {:style {:white-space   :nowrap
            :overflow      :hidden
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

(defn ^:private save-show [show]
  (store/dispatch (admin.actions/save-show show)))

(defn show-form [form-id {:keys [forms locations search]}]
  (when-let [form (get forms @form-id)]
    (let [locations (map (juxt :id identity) locations)
          locations-by-id (into {} locations)
          location-options (->> locations
                                (sort-by (comp :name second))
                                (filter (comp (partial re-find (re-pattern (str "(?i)" search))) str :name second)))]
      [:div
       [:div.row.full.spaced
        [:div.row.spaced
         [:a.link {:href (nav/path-for :ui.admin/main)} "Manage shows"]
         [:span.link-divider]
         [:a.link {:href (nav/path-for :ui.admin/calendar)} "Manage calendar events"]]
        [auth/logout]]
       [fields/form {:on-submit save-show
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
                          (forms/with-attrs form [:website]))]]])))

(defn root [{:keys [page]}]
  (let [show-id (get-in page [:route-params :show-id])
        form-id (r/atom nil)]
    (v/await [{[_ show] :show} (some-> show-id
                                       admin.actions/fetch-show
                                       store/dispatch)
              {:keys [summary start]} (:temp-data show)]
      (reset! form-id (-> show
                          (maps/default :name summary)
                          (update :date-time #(or %
                                                  (some-> start :dateTime)
                                                  (some-> start :date mo/parse)))
                          (select-keys #{:confirmed? :hidden? :location-id :name :date-time :website :id})
                          (admin.actions/create-form validator)
                          store/dispatch)))
    (store/dispatch admin.actions/fetch-locations)
    (partial components/with-status (cond-> #{:locations} show-id (conj :show)) [show-form form-id])))
