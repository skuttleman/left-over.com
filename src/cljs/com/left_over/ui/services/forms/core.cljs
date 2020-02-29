(ns com.left-over.ui.services.forms.core
  (:require
    [com.left-over.common.utils.logging :as log]))

(defn ^:private derefable? [value]
  (satisfies? IDeref value))

(defn ^:private swapable? [value]
  (satisfies? ISwap value))

(defprotocol IBlock
  (ready? [this]))

(defprotocol ISync
  (save! [this]))

(defprotocol IClean
  (clean! [this]))

(defprotocol IChange
  (changed? [this] [this path]))

(defprotocol ITrack
  (attempt! [this])
  (attempted? [this])
  (visit! [this path])
  (visited? [this path]))

(defprotocol IValidate
  (errors [this]))

(defn valid? [form]
  (empty? (errors form)))

(defn with-attrs
  ([attrs form path]
   (with-attrs attrs form path nil nil))
  ([attrs form path model->view view->model]
   (let [attempted? (when (satisfies? ITrack form)
                      (attempted? form))
         visited? (when (satisfies? ITrack form)
                    (visited? form path))
         errors (when (satisfies? IValidate form)
                  (get-in (errors form) path))
         to-view (get-in model->view path)
         to-model (get-in view->model path)]
     (-> attrs
         (assoc :attempted? attempted? :visited? visited?)
         (update :disabled #(or % (not (ready? form))))
         (update :on-blur (fn [on-blur]
                            (fn [e]
                              (visit! form path)
                              (when on-blur
                                (on-blur e)))))
         (cond->
           (derefable? form)
           (assoc :value (get-in @form path))

           (swapable? form)
           (assoc :on-change (partial swap! form assoc-in path))

           (and visited? errors)
           (assoc :errors errors)

           to-view (update :value to-view)
           to-model (update :on-change comp to-model))))))
