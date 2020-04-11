(ns com.left-over.api.services.db.migrations
  (:refer-clojure :exclude [load-file])
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.shared.utils.dates :as dates]
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
  (dates/format (dates/now) :datetime/fs dates/utc))

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

(defn ^:private run-migrations! [fmt alter-migrations migrations]
  (reduce (fn [p {migration-id :id}]
            (v/and p
                   (v/await [sql (-> fmt
                                     (strings/format migration-id)
                                     load-file)]
                     (repos/transact (fn [conn]
                                       (println "running:" (strings/format fmt migration-id))
                                       (->> (string/split sql #";")
                                            (remove string/blank?)
                                            (map (comp vector string/trim))
                                            (cons [alter-migrations migration-id])
                                            (map (partial repos/exec-raw! conn))
                                            v/all))))))
          (v/resolve)
          migrations))

(defn ^:private update-entities! []
  (v/then-> (repos/transact repos/exec-raw! [select-entities])
            (->> (map (juxt :table_name :column_name))
                 (reduce (fn [entities [table column]]
                           (update entities
                                   table
                                   (fnil conj #{})
                                   (keywords/keyword column)))
                         {})
                 (map (fn [[table fields]]
                        (write-file (strings/format "resources/db/entities/%s.edn" table)
                                    (pr-str {:table  (keywords/keyword table)
                                             :fields fields}))))
                 v/all)))

(defn migrate! []
  (v/await [[files migrations] (v/all [(list-migrations "resources/db/migrations")
                                       (repos/transact repos/exec-raw! [select-migrations-query])])
            ids (into #{} (map :id) migrations)
            migrations (drop-while (comp ids :id) files)]
    (run-migrations! "resources/db/migrations/%s.up.sql" insert-migration migrations)
    (update-entities!)))

(defn rollback!
  ([]
   (rollback! 1))
  ([n]
   (v/await [migrations (repos/transact repos/exec-raw! [select-migrations-limit-query n])]
     (run-migrations! "resources/db/migrations/%s.down.sql" delete-migration migrations)
     (update-entities!))))

(defn redo! []
  (v/and (rollback!)
         (migrate!)))

(defn speedbump! []
  (v/and (migrate!)
         (redo!)))

(defn create! [& name-parts]
  (let [migration-name (string/join "_" (cons (date-str) (map csk/->snake_case_string name-parts)))]
    (v/and (v/all [(write-file (strings/format "resources/db/migrations/%s.up.sql" migration-name) "")
                   (write-file (strings/format "resources/db/migrations/%s.down.sql" migration-name) "")])
           (println "created migration: " migration-name))))

(defn ^:private print-result [promise]
  (v/peek promise
          (fn [[status :as value]]
            (println (case status
                       :success [:success]
                       value)))))

(comment
  (print-result (create! "a new migration"))
  (print-result (migrate!))
  (print-result (speedbump!))
  (print-result (rollback!))
  (print-result (redo!)))
