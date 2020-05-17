(ns com.left-over.api.services.fs
  (:require
    [com.ben-allred.vow.core :as v]
    fs))

(defn ^:private promise-cb [resolve reject]
  (fn [error result]
    (if error
      (reject error)
      (resolve result))))

(defn read-file [file-name]
  (v/create (fn [resolve reject]
              (fs/readFile file-name (promise-cb resolve reject)))))

(defn write-file [file-name content]
  (v/create (fn [resolve reject]
              (fs/writeFile file-name content (promise-cb resolve reject)))))

(defn read-dir [dir]
  (v/create (fn [resolve reject]
              (fs/readdir dir (promise-cb resolve reject)))))
