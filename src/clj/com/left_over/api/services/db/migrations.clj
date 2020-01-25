(ns com.left-over.api.services.db.migrations
  (:gen-class)
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as string]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.common.utils.dates :as dates]
    [com.left-over.common.utils.numbers :as numbers]
    [ragtime.jdbc :as rag-db]
    [ragtime.repl :as rag]))

(defn ^:private date-str []
  (-> (dates/now)
      (dates/format :datetime/fs)))

(defn load-config []
  {:datastore  (rag-db/sql-database repos/db-cfg)
   :migrations (rag-db/load-resources "db/migrations")})

(defn migrate! []
  (rag/migrate (load-config)))

(defn rollback!
  ([]
   (rollback! 1))
  ([n]
   (let [cfg (load-config)]
     (loop [rollbacks n]
       (when (pos? rollbacks)
         (rag/rollback cfg)
         (recur (dec rollbacks)))))))

(defn redo! []
  (rollback!)
  (migrate!))

(defn speedbump! []
  (migrate!)
  (redo!))

(defn create! [name]
  (let [migration-name (format "%s_%s" (date-str) (csk/->snake_case_string name))]
    (spit (format "resources/db/migrations/%s.up.sql" migration-name) "\n")
    (spit (format "resources/db/migrations/%s.down.sql" migration-name) "\n")
    (println "created migration: " migration-name)))

(defn ^:export -main [& [command arg :as args]]
  (case command
    "migrate" (migrate!)
    "rollback" (rollback! (numbers/parse-int! (or arg "1")))
    "speedbump" (speedbump!)
    "redo" (redo!)
    "create" (create! (string/join "_" args))
    (throw (ex-info (str "unknown command: " command) {:command command :args args}))))
