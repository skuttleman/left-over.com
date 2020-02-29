(ns com.left-over.ui.views.dropdown
  (:require
    [clojure.set :as set]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.common.utils.maps :as maps]
    [com.left-over.ui.admin.views.fields :as fields]
    [com.left-over.ui.views.components :as components]))

(defn option-list [{:keys [close-on-change? item-control on-change on-search on-toggle options value]}]
  (let [options (concat (filter (comp (partial contains? value) first) options)
                        (remove (comp (partial contains? value) first) options))]
    [:ul.dropdown-items.lazy-list
     (for [[id display] options
           :let [selected? (contains? value id)]]
       ^{:key id}
       [:li.dropdown-item.pointer
        {:class    [(when selected? "is-active")]
         :on-click (fn [e]
                     (.stopPropagation e)
                     (let [next (if (contains? value id)
                                  (disj value id)
                                  ((fnil conj #{}) value id))]
                       (on-change next)
                       (when (and close-on-change? (seq next))
                         (when on-search (on-search nil))
                         (on-toggle next))))}
        [components/render item-control display]])]))

(defn display-text [attrs]
  (let [selected-count (count (:selected attrs))]
    [:span {:style {:white-space   :nowrap
                    :overflow      :hidden
                    :text-overflow :ellipsis}}
     (case selected-count
       0 "Select…"
       1 "1 Item Selected"
       (str selected-count " Items Selected"))]))

(defn button [{:keys [display-text-fn] :or {display-text-fn display-text} :as attrs}]
  [:button.button
   (-> attrs
       (select-keys #{:class :disabled :on-blur :on-click})
       (assoc :type :button))
   [display-text-fn attrs]
   [:span
    {:style {:margin-left "10px"}}
    [components/icon (if (:open? attrs) :chevron-up :chevron-down)]]])

(defn ^:private dropdown* [{:keys [button-control loading? list-control on-edit open? options options-by-id ref value]
                            :or   {list-control option-list button-control button}
                            :as   attrs}
                           search-input]
  (let [selected (seq (map options-by-id value))]
    [:div.dropdown.row.space-between
     {:class [(when open? "is-active")]
      :ref   ref}
     [:div.dropdown-trigger
      [components/render
       button-control
       (-> attrs
           (set/rename-keys {:on-toggle :on-click})
           (cond->
             selected (assoc :selected selected)
             open? (update :class conj "is-focused")))]]
     (when on-edit
       [:<>
        [:button.button.is-link {:tab-index -1 :type :button :on-click #(on-edit nil)} "Create"]
        (when (= 1 (count value))
          [:button.button.is-link {:tab-index -1 :type :button :on-click #(on-edit (first selected))} "Edit"])])
     (when open?
       [:div.dropdown-menu
        [:div.dropdown-content
         (when search-input
           [:div.dropdown-search search-input])
         [:div.dropdown-body
          (cond
            loading?
            [components/spinner]

            (seq options)
            [list-control attrs]

            :else
            [components/alert :info "No results"])]]])]))

(defn dropdown [_]
  (fn [{:keys [options] :as attrs}]
    (let [options-by-id (or (:options-by-id attrs) (into {} options))]
      [fields/form-field
       attrs
       [fields/openable [dropdown* (assoc attrs :options-by-id options-by-id)]]])))

(defn singleable [{:keys [value] :as attrs}]
  (let [value' (if (nil? value) #{} #{value})]
    (-> attrs
        (assoc :value value' :close-on-change? true)
        (update :on-change comp (comp first (partial remove value'))))))
