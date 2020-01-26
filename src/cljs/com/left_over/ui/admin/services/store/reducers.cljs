(ns com.left-over.ui.admin.services.store.reducers
  (:require
    [com.ben-allred.collaj.reducers :as cr]
    [com.left-over.ui.services.store.reducers :as reducers]))

(defn auth
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :auth.info/request) [:init]
     :auth.info/success [:success result]
     :auth.info/failure [:error result]
     state)))

(def reducer (cr/combine {:auth auth
                          :page reducers/page}))
