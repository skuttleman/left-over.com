(ns com.left-over.test.macros
  (:require
    [clojure.test :as t]
    [com.ben-allred.vow.core :as v]
    [com.left-over.shared.utils.logging :as log]))

(defmacro with-retry [tries & body]
  `(v/then (v/resolve ~tries)
           (fn continue# [n#]
             (v/catch (v/and (v/resolve) ~@body)
                      (fn [err#]
                        (v/and (v/sleep 100)
                               (if (zero? n#)
                                 (v/reject err#)
                                 (do (log/warn "retrying..." n#)
                                     (continue# (dec n#))))))))))

(defmacro testing [string & body]
  `(v/then (v/and (v/resolve)
                  (t/update-current-env! [:testing-contexts] conj ~string)
                  ~@body)
           (fn [result#]
             (t/update-current-env! [:testing-contexts] rest)
             result#)
           (fn [err#]
             (let [err2# (or (some-> err# .-stack) err#)
                   ctx# (update-in (t/get-current-env) [:report-counters :error] (fnil inc 0))]
               (t/update-current-env! [:testing-contexts] rest)
               (v/reject (if (map? err#) err2# (assoc ctx# :err err2#)))))))
