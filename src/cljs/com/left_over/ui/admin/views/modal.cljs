(ns com.left-over.ui.admin.views.modal
  (:require
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.admin.services.store.actions :as admin.actions]
    [com.ben-allred.vow.core :as v]))

(defn ^:private hide-modal [e]
  (store/dispatch admin.actions/hide-modal)
  e)

(defn ^:private wrap-attrs [[tag & args]]
  (let [attrs? (first args)
        [attrs args] (if (map? attrs?)
                       [attrs? (rest args)]
                       [{} args])]
    (into [tag (update attrs :on-click #(if % (comp % hide-modal) hide-modal))]
          args)))

(defn ^:private focus-first [[action :as actions]]
  (cond-> actions
    (and (vector? action) (map? (second action)))
    (->> rest
         (cons (update action 1 assoc :auto-focus true)))))

(defn modal [{:keys [state content title actions]}]
  (when (not= :unmounted state)
    [:div.modal.is-active
     {:on-click hide-modal
      :class    [(name state)]}
     [:div.modal-background]
     [:div.modal-content
      {:on-click #(.stopPropagation %)}
      [:div.card
       (when title
         [:div.card-header
          [:div.card-header-title title]])
       [:div.card-content
        content]
       (when (seq actions)
         (->> actions
              focus-first
              (into [:div.card-footer]
                    (map (comp (partial conj [:div.card-footer-item])
                               wrap-attrs)))))]]
     (when (#{:mounted :shown} state)
       [:button.modal-close.is-large
        {:aria-label :close}])]))
