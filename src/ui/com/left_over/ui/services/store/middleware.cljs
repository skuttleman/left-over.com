(ns com.left-over.ui.services.store.middleware)

(defn remove-empty-albums [_get-state]
  (fn [next]
    (fn [[type albums :as action]]
      (next (case type
              :songs/success [type (remove (comp empty? :songs) albums)]
              action)))))
