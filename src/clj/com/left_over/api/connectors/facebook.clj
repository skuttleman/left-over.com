(ns com.left-over.api.connectors.facebook
  (:require
    [clojure.java.io :as io]
    [clojure.set :as set]
    [com.left-over.common.utils.json :as json]
    [com.left-over.common.utils.maps :as maps])
  (:import
    (java.util Date)))

(defn ^:private json [file]
  (json/parse (io/resource file)))

(defn ^:private fetch-old [results page]
  (let [stub (json (format "stubs/fb-event-past-%d.json" page))
        next-results (into results (get-in stub [:data :page :past_events :edges]))]
    (if (get-in stub [:data :page :past_events :page_info :has_next_page])
      (recur next-results (inc page))
      next-results)))

(defn ^:private fetch-new []
  (get-in (json "stubs/fb-event-upcoming.json") [:data :page :upcoming_events :edges]))

(defn ^:private re-format [shows]
  (sequence (comp (map :node)
                  (remove (some-fn :is_event_draft :is_canceled))
                  (map #(-> %
                            (maps/select-renamed-keys {:id                       :id
                                                       :name                     :name
                                                       :timezone                 :timezone
                                                       :startTimestampForDisplay :date-time
                                                       :event_place              :location})
                            (update :date-time (fn [seconds] (Date. ^Long (* 1000 seconds))))
                            (update :location maps/select-renamed-keys {:contextual_name :name :city :city})
                            (update-in [:location :city] :contextual_name))))
            shows))

(defn past-shows []
  (re-format (fetch-old [] 1)))

(defn upcoming-shows []
  (re-format (fetch-new)))
