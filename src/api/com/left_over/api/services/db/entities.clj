(ns com.left-over.api.services.db.entities
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.db.entities :as ent]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.utils.keywords :as keywords]))

(def ^:private query
  "SELECT column_name
   FROM information_schema.columns
   WHERE table_schema='public'
     AND table_name='%s'")

(defn ^:private load-fields [table]
  (v/deref! (repos/transact (fn [db]
                              (-> (repos/exec-raw! db [(format query table)])
                                  (v/then-> (->> (into #{} (map (comp keywords/keyword :columns/column_name)))
                                                 (assoc {:table (keyword table)} :fields))))))))

(def locations (load-fields "locations"))
(defmacro loaded-locations [] locations)
(def shows (load-fields "shows"))
(defmacro loaded-shows [] shows)
(def users (load-fields "users"))
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
