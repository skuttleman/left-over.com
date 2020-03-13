(ns com.left-over.common.services.db.entities
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.set :as set]
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.utils.keywords :as keywords]
    [com.left-over.common.utils.strings :as strings]))

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

(declare shows locations users)

(defn ^:private make-entity [entity]
  (repos/transact (fn [conn]
                    (-> entity
                        (->> (strings/format "SELECT column_name
                                              FROM information_schema.columns
                                              WHERE table_schema='public'
                                                AND table_name='%s'")
                             vector
                             (repos/exec-raw! conn))
                        (v/then (fn [result]
                                  (->> result
                                       (into #{} (map (comp csk/->kebab-case-keyword #?(:clj  :columns/column_name
                                                                                        :cljs :column_name))))
                                       (assoc {:table (keyword entity)} :fields))))))))

(defonce ^:private _shows
  (v/then (make-entity "shows")
          (fn [entity]
            (def shows entity))))

(defonce ^:private _locations
  (v/then (make-entity "locations")
          (fn [entity]
            (def locations entity))))

(defonce ^:private _users
  (v/then (make-entity "users")
          (fn [entity]
            (def users entity))))
