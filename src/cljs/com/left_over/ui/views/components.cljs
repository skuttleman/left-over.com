(ns com.left-over.ui.views.components
  (:require
    [com.left-over.common.utils.colls :as colls]))

(def ^:private level->class
  {:error "is-danger"})

(def ^:private style->class
  {:solid "fas"
   :brand "fab"})

(defn spinner []
  [:div.loader-container [:div.loader.large]])

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(defn render [component & more-args]
  (-> component
      (or :<>)
      colls/force-vector
      (into more-args)))

(defn render-with-attrs [component attrs & more-args]
  (-> component
      colls/force-vector
      (update 1 merge attrs)
      (into more-args)))

(defn icon
  ([icon-class]
   (icon {} icon-class))
  ([attrs icon-class]
   [:i (update attrs :class conj (style->class (:icon-style attrs :solid)) (str "fa-" (name icon-class)))]))

(defn with-status [keys component state]
  (condp #(contains? %2 %1) (into #{} (map (comp first state)) keys)
    :error [:div
            [:p "something went wrong"]
            [:p "please try again later"]]
    :init [spinner]
    (conj (colls/force-vector component) (into state (map (juxt identity (comp second state))) keys))))
