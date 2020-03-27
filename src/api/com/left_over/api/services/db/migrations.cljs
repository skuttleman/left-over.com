(ns com.left-over.api.services.db.migrations
  (:refer-clojure :exclude [load-file])
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.keywords :as keywords]
    [com.left-over.shared.utils.strings :as strings]
    fs))

(def ^:private select-migrations-query
  "SELECT *
   FROM ragtime_migrations
   ORDER BY id ASC")

(def ^:private select-migrations-limit-query
  "SELECT *
   FROM ragtime_migrations
   ORDER BY id DESC
   LIMIT $1")

(def ^:private insert-migration
  "INSERT INTO ragtime_migrations (id, created_at)
   VALUES ($1, now())")

(def ^:private delete-migration
  "DELETE FROM ragtime_migrations
   WHERE id = $1")

(def ^:private select-entities
  "SELECT table_name, column_name
   FROM information_schema.columns
   WHERE table_schema='public'
     AND table_name != 'ragtime_migrations'")

(defn ^:private date-str []
  (let [now (js/Date.)]
    (strings/format
      "%04d%02d%02d%02d%02d%02d"
      (.getFullYear now)
      (inc (.getMonth now))
      (.getDate now)
      (.getHours now)
      (.getMinutes now)
      (.getSeconds now))))

(defn ^:private promise-cb [resolve reject]
  (fn [error result]
    (if error
      (reject error)
      (resolve result))))

(defn ^:private load-file [file-name]
  (v/create (fn [resolve reject]
              (fs/readFile file-name (promise-cb resolve reject)))))

(defn ^:private write-file [file-name content]
  (v/create (fn [resolve reject]
              (fs/writeFile file-name content (promise-cb resolve reject)))))

(defn ^:private list-migrations [dir]
  (v/then-> (v/create (fn [resolve reject]
                        (fs/readdir dir (promise-cb resolve reject))))
            (->> sort
                 (partition-all 2)
                 (map (fn [[down up]] {:down down
                                       :up   up
                                       :id   (first (string/split down #"\."))})))))

(defn ^:private run-migrations! [fmt alter-migrations]
  (fn [migrations]
    (reduce (fn [p {migration-id :id}]
              (-> p
                  (v/then (fn [_]
                            (-> fmt
                                (strings/format migration-id)
                                load-file)))
                  (v/then (fn [sql]
                            (repos/transact (fn [conn]
                                              (println "running:" (strings/format fmt migration-id))
                                              (->> (string/split sql #";")
                                                   (remove string/blank?)
                                                   (map (comp vector string/trim))
                                                   (cons [alter-migrations migration-id])
                                                   (map (partial repos/exec-raw! conn))
                                                   v/all)))))))
            (v/resolve)
            migrations)))

(defn ^:private update-entities [_]
  (v/then-> (repos/transact (fn [conn]
                              (repos/exec-raw! conn [select-entities])))
            (->> (map (juxt :table_name :column_name))
                 (reduce (fn [entities [table column]]
                           (update entities
                                   table
                                   (fnil conj #{})
                                   (keywords/keyword column)))
                         {})
                 (map (fn [[table fields]]
                        (write-file (strings/format "resources/db/entities/%s.edn" table)
                                    (pr-str {:table (keywords/keyword table)
                                             :fields fields}))))
                 v/all)))

(defn migrate! []
  (-> {:files      (list-migrations "resources/db/migrations")
       :migrations (repos/transact (fn transact [conn]
                                     (repos/exec-raw! conn [select-migrations-query])))}
      v/all
      (v/then (fn [{:keys [files migrations]}]
                (let [ids (into #{} (map :id) migrations)]
                  (drop-while (comp ids :id) files))))
      (v/then (run-migrations! "resources/db/migrations/%s.up.sql" insert-migration))
      (v/then update-entities)))

(defn rollback!
  ([]
   (rollback! 1))
  ([n]
   (-> (repos/transact (fn transact [conn]
                         (repos/exec-raw! conn [select-migrations-limit-query n])))
       (v/then (run-migrations! "resources/db/migrations/%s.down.sql" delete-migration))
       (v/then update-entities))))

(defn redo! []
  (v/then (rollback!) (fn [_] (migrate!))))

(defn speedbump! []
  (v/then (migrate!) (fn [_] (redo!))))

(defn create! [& name-parts]
  (let [migration-name (string/join "_" (cons (date-str) (map csk/->snake_case_string name-parts)))]
    (-> [(write-file (strings/format "resources/db/migrations/%s.up.sql" migration-name) "")
         (write-file (strings/format "resources/db/migrations/%s.down.sql" migration-name) "")]
        v/all
        (v/then (fn [_]
                  (println "created migration: " migration-name))))))

(comment
  (create! "migration")
  (migrate!)
  (speedbump!)
  (rollback!)
  (redo!))
