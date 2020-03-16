(ns com.left-over.common.services.db.entities.core
  (:require
    [clojure.set :as set]
    [com.left-over.shared.utils.keywords :as keywords]))

(defn ^:private with-field-alias [fields table-alias field-aliases]
  (let [table-alias' (name table-alias)]
    (map (fn [field]
           (let [field' (name field)
                 col (keyword (str table-alias' "." field'))]
             [col (keywords/str (get field-aliases field (str table-alias' "/" field')))]))
         fields)))

(defn ^:private join* [query join entity alias on aliases]
  (-> query
      (update join (fnil conj []) [(:table entity) alias] on)
      (update :select into (with-field-alias (:fields entity) alias aliases))))

(defn with-alias
  ([entity alias]
   (with-alias entity alias {}))
  ([entity alias aliases]
   (-> entity
       (update :select with-field-alias alias aliases)
       (update :from (comp vector conj) alias))))

(defn insert-into [entity rows]
  {:insert-into (:table entity)
   :values      rows
   :returning   [:*]})

(defn upsert [entity rows conflict keys]
  (-> entity
      (insert-into rows)
      (assoc :on-conflict conflict)
      (assoc :do-update-set (or keys (take 1 conflict)))))

(defn on-conflict-nothing [query conflict]
  (assoc query :on-conflict conflict :do-update-set (take 1 conflict)))

(defn limit [query amt]
  (assoc query :limit amt))

(defn offset [query amt]
  (assoc query :offset amt))

(defn modify [entity m clause]
  {:update    (:table entity)
   :set       m
   :where     clause
   :returning [:*]})

(defn select [entity]
  (-> entity
      (set/rename-keys {:fields :select :table :from})
      (update :from vector)))

(defn order [query column direction]
  (update query :order-by (fnil conj []) [column direction]))

(defn left-join
  ([query entity on]
   (left-join query entity (:table entity) on))
  ([query entity alias on]
   (left-join query entity alias on {}))
  ([query entity alias on aliases]
   (join* query :left-join entity alias on aliases)))

(defn inner-join
  ([query entity on]
   (inner-join query entity (:table entity) on))
  ([query entity alias on]
   (inner-join query entity alias on {}))
  ([query entity alias on aliases]
   (join* query :join entity alias on aliases)))

