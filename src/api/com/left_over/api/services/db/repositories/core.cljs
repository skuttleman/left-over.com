(ns com.left-over.api.services.db.repositories.core
  (:require
    [clojure.core.async :as async]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.env :as env]
    [com.left-over.shared.services.protocols :as p]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.logging :as log]
    [honeysql.core :as hsql]
    [honeysql.format :as hsql.fmt]
    honeysql-postgres.format
    honeysql-postgres.helpers
    pg-types))

(def Client (.-Client (nodejs/require "pg")))

(pg-types/setTypeParser pg-types/builtins.UUID #(some-> % uuid))

(aset (.-prototype UUID) "toPostgres" (fn []
                                        (this-as this
                                          (str this))))

(defn ^:private sql-value* [table column _]
  [table column])
(defmulti ->api (comp first vector))
(defmulti ->db (comp first vector))

(defmulti ->sql-value #'sql-value*)
(defmethod ->api :default [_ value] value)
(defmethod ->db :default [_ value] value)

(defmethod ->sql-value :default [_ _ value] value)

(defmethod hsql.fmt/format-clause :json/merge [[_ k] sql-map]
  (let [[param] (get (meta sql-map) k)
        s (if (:set sql-map) ", " "SET ")
        k' (hsql.fmt/to-sql k)]
    (str s k' " = " k' " || " param)))

(hsql.fmt/register-clause! :json/merge 101)

(defn ^:private sql-log [[statement]]
  (async/go
    (when (env/get :dev?)
      (log/debug statement))))

(extend-protocol p/IDbConnection
  Client
  (query [this sql params]
    (-> this
        (.query sql (to-array params))
        v/native->prom
        (v/then-> .-rows)))
  (prepare [_ query]
    (->> query
         meta
         vals
         (reduce (fn [sql+params [param value]]
                   (-> sql+params
                       (update 0 string/replace param (str "$" (count sql+params)))
                       (conj value)))
                 (hsql/format query :quoting :ansi :parameterizer :postgresql)))))

(defn ^:private end-with [client sql]
  (fn [_]
    (-> client
        (p/query sql [])
        (v/and (.end client)))))

(defn exec-raw!
  ([conn sql+params]
   (exec-raw! conn sql+params identity))
  ([conn [sql & params] xform]
   (v/await [results (p/query conn sql params)]
     (sequence (comp (map #(js->clj % :keywordize-keys true))
                     xform)
               results))))

(defn ^:private exec* [conn query xform]
  (let [sql (p/prepare conn query)]
    (sql-log sql)
    (exec-raw! conn sql xform)))

(def ^:private remove-namespaces
  (map (fn remove* [x]
         (cond
           (map? x) (into {} (map (juxt (comp keyword name key) (comp remove* val))) x)
           (coll? x) (map remove* x)
           :else x))))

(defn exec! [query conn]
  (let [[query' xform-before xform-after] (colls/force-sequential query)
        xform (cond-> remove-namespaces
                xform-before (->> (comp xform-before))
                xform-after (comp xform-after))]
    (exec* conn query' xform)))

(defn ^:private transact* [f]
  (let [client (Client. (-> {:host     (env/get :db-host)
                             :port     (env/get :db-port)
                             :database (env/get :db-name)
                             :user     (env/get :db-user)
                             :password (env/get :db-password)}
                            (cond->
                              (not= "localhost" (env/get :db-host))
                              (assoc-in [:ssl :rejectUnauthorized] false))
                            clj->js))]
    (-> client
        .connect
        v/native->prom
        (v/and (p/query client "BEGIN" [])
               (f client))
        (v/peek (end-with client "COMMIT")
                (end-with client "ROLLBACK")))))

(defn transact
  ([f]
   (transact* f))
  ([f arg]
   (transact* #(f % arg)))
  ([f arg & more]
   (transact* #(apply f % arg more))))
