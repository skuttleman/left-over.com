(ns com.left-over.api.services.db.entities
  (:require
    [clojure.set :as set]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.keywords :as keywords]
    fs))

(defn ^:private with-field-alias [fields table-alias field-aliases]
  (let [table-alias' (name table-alias)]
    (map (fn [field]
           (let [field' (name field)
                 col (keyword (str table-alias' "." field'))]
             [col (keywords/str (get field-aliases field (str table-alias' "/" field')))]))
         fields)))

(defn ^:private join* [query join entity alias on aliases]
  (-> query
      (update join (fnil conj []) [(:table entity) alias] on)
      (update :select into (with-field-alias (:fields entity) alias aliases))))

(defn with-alias
  ([entity alias]
   (with-alias entity alias {}))
  ([entity alias aliases]
   (-> entity
       (update :select with-field-alias alias aliases)
       (update :from (comp vector conj) alias))))

(defn insert-into [entity rows]
  {:insert-into (:table entity)
   :values      rows
   :returning   [:*]})

(defn upsert [entity rows conflict keys]
  (-> entity
      (insert-into rows)
      (assoc :on-conflict conflict)
      (assoc :do-update-set (or keys (take 1 conflict)))))

(defn on-conflict-nothing [query conflict]
  (assoc query :on-conflict conflict :do-update-set (take 1 conflict)))

(defn limit [query amt]
  (assoc query :limit amt))

(defn offset [query amt]
  (assoc query :offset amt))

(defn modify
  ([entity clause]
   (modify entity nil clause))
  ([entity m clause]
   (cond-> {:update    (:table entity)
            :where     clause
            :returning [:*]}
     m (assoc :set m))))

(defn select [entity]
  (-> entity
      (set/rename-keys {:fields :select :table :from})
      (update :from vector)))

(defn json-merge [query field value]
  (-> query
      (assoc :json/merge field)
      (vary-meta assoc field [(str "*" (random-uuid) "*") (clj->js value)])))

(defn order [query column direction]
  (update query :order-by (fnil conj []) [column direction]))

(defn where [m clause]
  (if (contains? m :where)
    (update m :where (partial conj [:and clause]))
    (assoc m :where clause)))

(defn left-join
  ([query entity on]
   (left-join query entity (:table entity) on))
  ([query entity alias on]
   (left-join query entity alias on {}))
  ([query entity alias on aliases]
   (join* query :left-join entity alias on aliases)))

(defn inner-join
  ([query entity on]
   (inner-join query entity (:table entity) on))
  ([query entity alias on]
   (inner-join query entity alias on {}))
  ([query entity alias on aliases]
   (join* query :join entity alias on aliases)))

(def albums (edn/parse (fs/readFileSync "resources/db/entities/albums.edn")))

(def locations (edn/parse (fs/readFileSync "resources/db/entities/locations.edn")))

(def media-links (edn/parse (fs/readFileSync "resources/db/entities/media_links.edn")))

(def shows (edn/parse (fs/readFileSync "resources/db/entities/shows.edn")))

(def songs (edn/parse (fs/readFileSync "resources/db/entities/songs.edn")))

(def users (edn/parse (fs/readFileSync "resources/db/entities/users.edn")))
