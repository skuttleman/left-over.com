(ns com.left-over.common.services.db.repositories.core
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.core.async :as async]
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.env :as env]
    [com.left-over.common.utils.colls :as colls]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.common.utils.strings :as strings]
    [honeysql.core :as sql]))

(def Client (.-Client (nodejs/require "pg")))

(defn ^:private sql-value* [table column _]
  [table column])
(defmulti ->api (comp first vector))
(defmulti ->db (comp first vector))

(defmulti ->sql-value #'sql-value*)
(defmethod ->api :default [_ value] value)
(defmethod ->db :default [_ value] value)

(defmethod ->sql-value :default [_ _ value] value)

(defn ^:private sql-format [query]
  (sql/format query :quoting :ansi))

(defn ^:private sql-log [[statement & args]]
  (async/go
    (when true
      (env/get :dev?)
      (let [bindings (volatile! args)]
        (log/info
          (string/replace statement
                          #"(\(| )\?"
                          (fn [[_ prefix]]
                            (let [result (strings/format "%s'%s'" prefix (first @bindings))]
                              (vswap! bindings rest)
                              result))))))))

(defn ^:private query [conn [sql & params]]
  (v/native->prom (.query conn sql (to-array params))))

(defn exec-raw!
  ([conn sql+params]
   (exec-raw! conn sql+params identity))
  ([conn sql+params xform]
   (-> conn
       (query sql+params)
       (v/then #(.-rows %))
       (v/then (partial sequence (comp (map #(js->clj % :keywordize-keys true))
                                       xform))))))

(defn ^:private exec* [conn query xform]
  (let [sql (sql/format query :quoting :ansi :parameterizer :postgresql)]
    (sql-log sql)
    (exec-raw! conn sql xform)))

(def ^:private remove-namespaces
  (map (fn remove* [x]
         (cond
           (map? x) (into {} (map (juxt (comp keyword name key) (comp remove* val))) x)
           (coll? x) (map remove* x)
           (and (string? x) (re-matches #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" x)) (uuid x)
           :else x))))

(defn exec! [query conn]
  (let [[query' xform-before xform-after] (colls/force-sequential query)
        xform (cond-> remove-namespaces
                xform-before (->> (comp xform-before))
                xform-after (comp xform-after))]
    (exec* conn query' xform)))

(defn transact [f]
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
        (v/then (fn [_] (query client ["BEGIN"])))
        (v/then (fn [_] (f client)))
        (v/peek (fn [_] (query client ["COMMIT"]))
                (fn [_] (query client ["ROLLBACK"])))
        (v/peek (fn [_] (.end client))))))
