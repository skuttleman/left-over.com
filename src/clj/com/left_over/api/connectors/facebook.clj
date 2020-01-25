(ns com.left-over.api.connectors.facebook
  (:require
    [clojure.java.io :as io]
    [com.left-over.common.utils.dates :as dates]
    [com.left-over.common.utils.json :as json]
    [com.left-over.common.utils.maps :as maps])
  (:import
    (java.util Date)))

(defn ^:private json [file]
  (json/parse (io/resource file)))

(defn ^:private fetch* [stub k]
  (get-in (json stub) [:data :page k :edges]))

(defn ^:private re-format [shows]
  (sequence (comp (map :node)
                  (remove (some-fn :is_event_draft :is_canceled))
                  (map (fn [event]
                         (if-let [date-time (some-> event :childEvents :edges first :node :currentStartTimestamp)]
                           (assoc event :startTimestampForDisplay date-time)
                           event)))
                  (map #(-> %
                            (maps/select-renamed-keys {:id                       :id
                                                       :name                     :name
                                                       :timezone                 :timezone
                                                       :timeContext              :dates
                                                       :startTimestampForDisplay :date-time
                                                       :event_place              :location})
                            (maps/update-maybe :date-time (fn [seconds] (Date. ^Long (* 1000 seconds))))
                            (update :location maps/select-renamed-keys {:contextual_name :name :city :city})
                            (update-in [:location :city] :contextual_name))))
            shows))

(defn ^:private upcoming* [compare other-shows]
  (->> [(fetch* "stubs/fb-event-recurring.json" :upcomingRecurringEvents)
        (fetch* "stubs/fb-event-upcoming.json" :upcoming_events)
        other-shows]
       (mapcat re-format)
       (sort-by :date-time compare)))

(defn past-shows []
  (->> (fetch* "stubs/fb-event-past.json" :past_events)
       (upcoming* #(compare %2 %1))
       (take-while (comp (complement pos?) (partial compare (dates/->inst (dates/now))) :date-time))))

(defn upcoming-shows []
  (->> []
       (upcoming* compare)
       (drop-while (comp pos? (partial compare (dates/->inst (dates/now))) :date-time))))
