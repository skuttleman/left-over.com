(ns com.left-over.ui.admin.services.store.reducers
  (:require
    [cljs.core.match :refer-macros [match]]
    [com.ben-allred.collaj.reducers :as cr]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.ui.services.store.reducers :as reducers]))

(defn auth
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :auth.info/request) [:init]
     :auth.info/success [:success result]
     :auth.info/failure [:error result]
     state)))

(defn forms
  ([] {})
  ([state action]
   (match action
     [:forms/init internal-id external-id form-state form] (-> state
                                                               (assoc-in [external-id] form)
                                                               (assoc-in [:forms/state internal-id] form-state))
     [:forms/swap! internal-id f & f-args] (apply update-in state [:forms/state internal-id] f f-args)
     :else state)))

(defn ^:private toasts
  ([] {})
  ([state [type {:keys [id level body]}]]
   (case type
     :toast/add (assoc state id {:state :init :level level :body body})
     :toast/show (maps/update-maybe state id assoc :state :showing)
     :toast/hide (maps/update-maybe state id assoc :state :removing)
     :toast/remove (dissoc state id)
     state)))

(defn ^:private locations
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :locations/request) [:init]
     :locations/success [:success result]
     :locations/failure [:error result]
     state)))

(defn ^:private show
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :show/request) [:init]
     :show/success [:success result]
     :show/failure [:error result]
     state)))

(defn ^:private calendar
  ([] [:init])
  ([state [type result]]
   (case type
     (:router/navigate :calendar/request) [:init]
     :calendar/success [:success result]
     :calendar/failure [:error result]
     state)))

(defn modal
  ([] {:state :unmounted})
  ([state [type content title actions]]
   (case type
     :modal/mount {:state :mounted :content content :title title :actions actions}
     :modal/show (assoc state :state :shown)
     :modal/hide (assoc state :state :modal-hidden)
     :modal/unmount {:state :unmounted}
     state)))

(defn search
  ([] nil)
  ([state [type value]]
   (case type
     :search/set value
     state)))

(def reducer (cr/combine {:auth      auth
                          :calendar  calendar
                          :forms     forms
                          :locations locations
                          :modal     modal
                          :page      reducers/page
                          :search    search
                          :shows     reducers/shows
                          :show      show
                          :toasts    toasts}))
