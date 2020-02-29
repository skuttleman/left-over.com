(ns com.left-over.ui.services.forms.standard
  (:require
    [com.left-over.ui.services.forms.core :as forms]
    [com.left-over.ui.services.store.core :as store]))

(defn ^:private diff-paths [paths path old-model new-model]
  (reduce-kv (fn [paths k v]
               (let [path (conj path k)]
                 (cond
                   (map? v) (diff-paths paths path (get old-model k) v)
                   (not= (get old-model k) v) (conj paths path)
                   :else paths)))
             paths
             new-model))

(defn ^:private nest [paths path model]
  (reduce-kv (fn [paths k v]
               (let [path (conj path k)]
                 (if (map? v)
                   (nest paths path v)
                   (assoc paths path v))))
             paths
             model))

(defn trackable->model [trackable]
  (reduce-kv (fn [model path {:keys [current]}]
               (assoc-in model path current))
             {}
             trackable))

(defn ^:private model->trackable [model]
  (->> model
       (nest {} [])
       (into {} (map (fn [[k value]]
                       [k {:current  value
                           :initial  value
                           :visited? false}])))))

(defn check-for [working pred]
  (loop [[val :as working] (vals working)]
    (if (empty? working)
      false
      (or (pred val) (recur (rest working))))))

(defn swap* [{:keys [working] :as state} validator f f-args]
  (let [current (trackable->model working)
        next (apply f current f-args)]
    (->> next
         (diff-paths #{} [] current)
         (reduce (fn [working path]
                   (update working path assoc :current (get-in next path)))
                 working)
         (assoc state
           :model next
           :errors (validator next)
           :working))))

(defn ->storage [model validator]
  {:errors             (validator model)
   :model              model
   :persist-attempted? false
   :status             :ready
   :working            (model->trackable model)})

(defrecord Form [dispatch validator internal-id external-id]
  forms/IBlock
  (ready? [_]
    (= :ready (get-in (store/get-state) [:forms :forms/state internal-id :status])))

  forms/IChange
  (changed? [_]
    (check-for (get-in (store/get-state) [:forms :forms/state internal-id :working])
               #(not= (:initial %) (:current %))))
  (changed? [_ path]
    (let [{:keys [current initial]} (get-in (store/get-state) [:forms :forms/state internal-id :working path])]
      (not= current initial)))

  forms/ITrack
  (attempt! [_]
    (dispatch [:forms/swap! internal-id assoc :persist-attempted? true])
    nil)
  (attempted? [_]
    (get-in (store/get-state) [:forms :forms/state internal-id :persist-attempted?]))
  (visit! [_ path]
    (dispatch [:forms/swap! internal-id assoc-in [:working path :visited?] true])
    nil)
  (visited? [_ path]
    (let [{:keys [working persist-attempted?]} (get-in (store/get-state) [:forms :forms/state internal-id])]
      (or persist-attempted?
          (get-in working [path :visited?]))))

  forms/IValidate
  (errors [this]
    (when (forms/ready? this)
      (let [{:keys [errors api-errors model]} (get-in (store/get-state) [:forms :forms/state internal-id])]
        (->> (for [[path m] api-errors
                   [value errors'] m
                   :when (= value (get-in model path))]
               [path errors'])
             (reduce (fn [m [path e]] (update-in m path concat e)) errors)))))

  IDeref
  (-deref [_]
    (get-in (store/get-state) [:forms :forms/state internal-id :model]))

  ISwap
  (-swap! [_ f]
    (dispatch [:forms/swap! internal-id swap* validator f []])
    nil)
  (-swap! [_ f a]
    (dispatch [:forms/swap! internal-id swap* validator f [a]])
    nil)
  (-swap! [_ f a b]
    (dispatch [:forms/swap! internal-id swap* validator f [a b]])
    nil)
  (-swap! [_ f a b xs]
    (dispatch [:forms/swap! internal-id swap* validator f (into [a b] xs)])
    nil))

(defn create [model validator dispatch internal-id external-id]
  (let [form (->Form dispatch validator internal-id external-id)]
    (dispatch [:forms/init internal-id external-id (->storage model validator) form])))
