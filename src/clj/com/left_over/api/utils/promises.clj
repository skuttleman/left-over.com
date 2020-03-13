(ns com.left-over.api.utils.promises)

(defn deref! [promise]
  (let [[status value] @promise]
    (cond
      (= :success status) value
      (instance? Throwable value) (throw value)
      :else (throw (ex-info "failed promise" {:error value})))))
