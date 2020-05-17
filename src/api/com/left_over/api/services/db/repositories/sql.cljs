(ns com.left-over.api.services.db.repositories.sql
  (:require
    [honeysql.core :as hsql]))

(defn call [f & args]
  (apply hsql/call f args))

(defn coalesce [& args]
  (apply call :coalesce args))

(defn cast [value type]
  (call :cast value (hsql/inline (name type))))

(defn json-get-in [col path]
  (apply hsql/call
         :jsonb_extract_path_text
         col
         (map name path)))
