(ns com.left-over.api.services.db.entities
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.db.entities :as ent]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.utils.keywords :as keywords]
    [clojure.string :as string]))

(declare locations shows users)

(def ^:private query
  "SELECT table_name, column_name
   FROM information_schema.columns
   WHERE table_schema='public'
     AND table_name IN (%s)")

(defn ^:private load-fields [tables]
  (v/deref! (repos/transact (fn [db]
                              (-> tables
                                  (->> (map #(str "'" % "'"))
                                       (string/join ",")
                                       (format query)
                                       vector
                                       (repos/exec-raw! db))
                                  (v/then-> (->> (map (juxt :columns/table_name :columns/column_name))
                                                 (reduce (fn [entities [table column]]
                                                           (update entities
                                                                   (keywords/keyword table)
                                                                   (fnil conj #{})
                                                                   (keywords/keyword column)))
                                                         {}))))))))

(let [entities (load-fields ["locations" "shows" "users"])]
  (def locations {:table :locations :fields (:locations entities)})
  (def shows {:table :shows :fields (:shows entities)})
  (def users {:table :users :fields (:users entities)}))
(defmacro loaded-locations [] locations)
(defmacro loaded-shows [] shows)
(defmacro loaded-users [] users)

(def ^{:arglists (:arglists (meta #'ent/with-alias))} with-alias ent/with-alias)
(def ^{:arglists (:arglists (meta #'ent/insert-into))} insert-into ent/insert-into)
(def ^{:arglists (:arglists (meta #'ent/upsert))} upsert ent/upsert)
(def ^{:arglists (:arglists (meta #'ent/on-conflict-nothing))} on-conflict-nothing ent/on-conflict-nothing)
(def ^{:arglists (:arglists (meta #'ent/limit))} limit ent/limit)
(def ^{:arglists (:arglists (meta #'ent/offset))} offset ent/offset)
(def ^{:arglists (:arglists (meta #'ent/modify))} modify ent/modify)
(def ^{:arglists (:arglists (meta #'ent/select))} select ent/select)
(def ^{:arglists (:arglists (meta #'ent/order))} order ent/order)
(def ^{:arglists (:arglists (meta #'ent/left-join))} left-join ent/left-join)
(def ^{:arglists (:arglists (meta #'ent/inner-join))} inner-join ent/inner-join)
