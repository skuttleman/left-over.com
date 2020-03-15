(ns com.left-over.api.services.db.entities
  (:require
    [com.left-over.common.services.db.entities :as ent])
  (:require-macros
    [com.left-over.api.services.db.entities]))

(def locations (com.left-over.api.services.db.entities/loaded-locations))
(def shows (com.left-over.api.services.db.entities/loaded-shows))
(def users (com.left-over.api.services.db.entities/loaded-users))

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
