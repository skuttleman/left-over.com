(ns com.left-over.api.services.db.models.core
  (:require
    [com.left-over.api.services.db.preparations :as prep]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.logging :as log]))

(defn ^:private with* [k f [pk fk] values]
  (let [pk->results (-> values
                        seq
                        (some->> (map #(get % pk)) f)
                        (->> (group-by fk)))]
    (fn [value]
      (assoc value k (pk->results (get value pk) [])))))

(defn under [root-key]
  (let [root-key' (name root-key)]
    (map (fn [item]
           (let [groups (group-by (comp namespace first) item)
                 others (dissoc groups nil root-key')
                 init (into {} (concat (get groups nil) (get groups root-key')))]
             (reduce (fn [m [k v]]
                       (if-let [v' (seq (filter (comp some? second) v))]
                         (assoc m k (into {} v'))
                         m))
                     init
                     others))))))

(defn select
  ([query model]
   (select query model nil))
  ([query model x-form]
   [query x-form (map (partial repos/->api model))]))

(defn insert-many [query entity model]
  (let [fields (disj (:fields entity) :created-at)]
    (update query :values (comp (partial map (comp (prep/prepare repos/->sql-value (:table entity))
                                                   #(select-keys % fields)
                                                   (partial repos/->db model)))
                                colls/force-sequential))))

(defn modify [query entity model]
  (let [fields (disj (:fields entity) :created-at :created-by :id)]
    (update query :set (comp (prep/prepare repos/->sql-value (:table entity))
                             #(select-keys % fields)
                             (partial repos/->db model)))))

(defn with [k f [pk fk] values]
  (map (with* k f [pk fk] values) values))
