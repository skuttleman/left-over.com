(ns com.left-over.ui.admin.views.fields
  (:require
    [clojure.set :as set]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.common.utils.maps :as maps]
    [com.left-over.common.utils.strings :as strings]
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.views.components :as components]
    [reagent.core :as r]
    [com.left-over.common.utils.colls :as colls]))

(defonce ^:private listeners (atom {}))

(def common-keys #{:auto-focus :class :id :on-blur :on-submit :ref :value :tab-index})

(def ^:private key->code
  {:key-codes/tab   9
   :key-codes/esc   27
   :key-codes/enter 13})

(def ^:private code->key
  (set/map-invert key->code))

(defn add-listener
  ([event cb]
   (add-listener event cb nil))
  ([event cb options]
   (let [key (gensym)
         listener [js/window event (.addEventListener js/window (name event) cb (clj->js options))]]
     (swap! listeners assoc key listener)
     key)))

(defn remove-listener [key]
  (when-let [[node event id] (get @listeners key)]
    (swap! listeners dissoc key)
    (.removeEventListener node (name event) id)))

(defn ^:private focus [node]
  (when node
    (.focus node)))

(defn ^:private blur [node]
  (when node
    (.blur node)))

(defn ^:private attempt [on-submit form]
  (fn [e]
    (.preventDefault e)
    (forms/attempt! form)
    (when-not (forms/errors form)
      (on-submit @form))))

(defn event->key [e]
  (-> e .-keyCode code->key))

(defn ^:private target-value [event]
  (some-> event .-target .-value))

(defn form-field [{:keys [attempted? errors form-field-class id label label-small? visited?]} & body]
  (let [errors (seq (remove nil? errors))
        show-errors? (and errors (or visited? attempted?))]
    [:div.form-field
     {:class (into [(when show-errors? "errors")] form-field-class)}
     [:<>
      (when label
        [:label.label
         (cond-> {:html-for id}
           label-small? (assoc :style {:font-weight :normal
                                       :font-size   "0.8em"}))
         label])
      (into [:div.form-field-control] body)]
     (when show-errors?
       [:ul.error-list
        (for [error errors]
          [:li.error
           {:key error}
           error])])]))

(defn ^:private with-auto-focus [component]
  (fn [{:keys [auto-focus?]} & _]
    (let [vnode (volatile! nil)
          ref (fn [node] (some->> node (vreset! vnode)))]
      (r/create-class
        {:component-did-update
         (fn [this _]
           (when-let [node @vnode]
             (when (and auto-focus? (not (:disabled (second (r/argv this)))))
               (vreset! vnode nil)
               (focus node))))
         :reagent-render
         (fn [attrs & args]
           (into [component (cond-> (dissoc attrs :auto-focus?)
                              auto-focus? (assoc :ref ref))]
                 args))}))))

(defn ^:private with-id [component]
  (fn [_attrs & _args]
    (let [id (gensym "form-field")]
      (fn [attrs & args]
        (into [component (assoc attrs :id id)] args)))))

(defn ^:private with-trim-blur [component]
  (fn [attrs & args]
    (-> attrs
        (update :on-blur (fn [on-blur]
                           (fn [e]
                             (when-let [on-change (:on-change attrs)]
                               (on-change (strings/trim-to-nil (:value attrs))))
                             (when on-blur
                               (on-blur e)))))
        (->> (conj [component]))
        (into args))))

(defn form [{:keys [form button-content] :or {button-content "Submit"} :as attrs} & body]
  (-> [:form.form (-> attrs
                      (select-keys common-keys)
                      (maps/update-maybe :on-submit attempt form))]
      (into body)
      (conj [:button.button.is-link {:type     :submit
                                     :disabled (and (forms/attempted? form)
                                                    (forms/errors form))}
             button-content])))

(def ^{:arglists '([attrs options])} select
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs} options]
        (let [option-values (set (map first options))
              value (if (contains? option-values value)
                      value
                      ::empty)]
          [form-field
           attrs
           [:select.select
            (-> {:value     (str value)
                 :disabled  disabled
                 :on-change (comp on-change
                                  (into {} (map (juxt str identity) option-values))
                                  target-value)}
                (merge (select-keys attrs #{:class :id :on-blur :ref})))
            (for [[option label attrs] (cond->> options
                                                (= ::empty value) (cons [::empty
                                                                         "Choose…"
                                                                         {:disabled true}]))
                  :let [str-option (str option)]]
              [:option
               (assoc attrs :value str-option :key str-option)
               label])]])))))

(def ^{:arglists '([attrs])} textarea
  (with-auto-focus
    (with-id
      (with-trim-blur
        (fn [{:keys [disabled on-change value] :as attrs}]
          [form-field
           attrs
           [:textarea.textarea
            (-> {:value     value
                 :disabled  disabled
                 :on-change (comp on-change target-value)}
                (merge (select-keys attrs common-keys)))]])))))

(def ^{:arglists '([attrs])} input
  (with-auto-focus
    (with-id
      (with-trim-blur
        (fn [{:keys [disabled on-change type] :as attrs}]
          [form-field
           attrs
           [:input.input
            (-> {:type      (or type :text)
                 :disabled  disabled
                 :on-change (comp on-change target-value)}
                (merge (select-keys attrs common-keys)))]])))))

(def ^{:arglists '([attrs])} checkbox
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs}]
        [form-field
         attrs
         [:input.checkbox
          (-> {:checked   (boolean value)
               :type      :checkbox
               :disabled  disabled
               :on-change #(on-change (not value))}
              (merge (select-keys attrs common-keys)))]]))))

(def ^{:arglists '([attrs])} button
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs} true-display false-display]
        [form-field
         attrs
         [:button.button
          (-> {:type     :button
               :disabled disabled
               :on-click #(on-change (not value))}
              (merge (select-keys attrs common-keys)))
          (if value true-display false-display)]]))))

(defn ^:private ref-fn [ref-fn ref]
  (fn [node]
    (when node
      (vreset! ref node))
    (when ref-fn
      (ref-fn node))))

(defn openable [_component]
  (let [open? (r/atom false)
        ref (volatile! nil)
        listeners [(add-listener :click
                                 (fn [e]
                                   (let [nodes (->> (.-target e)
                                                    (iterate #(some-> % .-parentNode))
                                                    (take-while some?)
                                                    (filter #{@ref}))]
                                     (if (empty? nodes)
                                       (do (reset! open? false)
                                           (some-> @ref blur))
                                       (some-> @ref focus)))))
                   (add-listener :keydown
                                 #(when (#{:key-codes/tab :key-codes/esc} (event->key %))
                                    (reset! open? false))
                                 true)]]
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (run! remove-listener listeners))
       :reagent-render
       (fn [component]
         (let [[_ {:keys [on-search search]}] (colls/force-vector component)
               attrs {:on-toggle (fn [_]
                                   (swap! open? not)
                                   (when (and (not @open?) on-search)
                                     (on-search nil)))
                      :open?     @open?}]
           (-> component
               (components/render-with-attrs attrs (when on-search
                                                     [input {:class :dropdown-search
                                                             :on-change   on-search
                                                             :value       search
                                                             :tab-index   -1
                                                             :auto-focus true}]))
               (update 1 #(-> %
                              (update :ref ref-fn ref)
                              (update :on-blur (fn [on-blur]
                                                 (fn [e]
                                                   (let [node @ref]
                                                     (cond
                                                       (and node @open?) (focus node)
                                                       on-blur (on-blur e)))))))))))})))
