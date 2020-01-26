(ns com.left-over.common.utils.dates
  (:refer-clojure :exclude [format])
  (:require
    #?@(:cljs [[java.time :refer [ZonedDateTime]]
               [java.time.format :refer [DateTimeFormatter]]
               cljs.java-time.extend-eq-and-compare])
    [cljc.java-time.day-of-week :as dow]
    [cljc.java-time.format.date-time-formatter :as dtf]
    [cljc.java-time.instant :as inst]
    [cljc.java-time.zone-id :as zi]
    [cljc.java-time.zone-offset :as zo]
    [cljc.java-time.zoned-date-time :as zdt]
    [clojure.string :as string]
    [tick.format :as tf]
    tick.locale-en-us)
  #?(:clj
     (:import
       (java.io Writer)
       (java.time ZonedDateTime)
       (java.time.format DateTimeFormatter)
       (java.util Date))))

(def ^:private formats
  {:datetime/view     "EEE, MMM d, yyyy h:mm a"
   :date/system       "yyyy-MM-dd"
   :date/view         "EEE, MMM d, yyyy"
   :date.no-year/view "EEE, MMM d"
   :datetime/fs       "yyyyMMddHHmmss"
   :date/year         "yyyy"
   :date/day          "d"
   :date/month        "MMM"
   :time/view         "h:mm a"})

(defn ^:private ->date [v]
  (cond
    (instance? ZonedDateTime v) v
    (string? v) (zdt/parse v)
    (inst? v) (zdt/of-instant (inst/of-epoch-milli (.getTime v)) zo/utc)))

(defn format
  ([inst]
   (format inst :datetime/view))
  ([inst fmt]
   (let [inst' (zdt/with-zone-same-instant (->date inst) (zo/system-default))]
     (-> fmt
         (formats fmt)
         (tf/formatter)
         (dtf/format inst')))))

(defn plus [inst? amt interval]
  (cond-> (->date inst?)
    (= :years interval) (zdt/plus-years amt)
    (= :months interval) (zdt/plus-months amt)
    (= :weeks interval) (zdt/plus-weeks amt)
    (= :days interval) (zdt/plus-days amt)
    (= :hours interval) (zdt/plus-hours amt)
    (= :minutes interval) (zdt/plus-minutes amt)
    (= :seconds interval) (zdt/plus-seconds amt)))

(defn minus [inst? amt interval]
  (plus inst? (* -1 amt) interval))

(defn with [inst? amt interval]
  (cond-> (->date inst?)
    (= :year interval) (zdt/with-year amt)
    (= :month interval) (zdt/with-month amt)
    (= :day interval) (zdt/with-day-of-month amt)
    (= :hour interval) (zdt/with-hour amt)
    (= :minute interval) (zdt/with-minute amt)
    (= :second interval) (zdt/with-second amt)))

(defn year [inst]
  (format inst :date/year))

(defn month [inst]
  (format inst :date/month))

(defn day [inst]
  (format inst :date/day))

(defn day-of-week [inst?]
  (-> inst?
      ->date
      (zdt/with-zone-same-instant (zo/system-default))
      zdt/get-day-of-week
      ({dow/sunday    :sunday
        dow/monday    :monday
        dow/tuesday   :tuesday
        dow/wednesday :wednesday
        dow/thursday  :thursday
        dow/friday    :friday
        dow/saturday  :saturday})))

(defn after? [date-1 date-2]
  (-> date-1
      (->date)
      (zdt/is-after (->date date-2))))

(defn before? [date-1 date-2]
  (-> date-1
      (->date)
      (zdt/is-before (->date date-2))))

(defn now []
  (zdt/now zo/utc))

(defn inst->ms [inst?]
  (inst/to-epoch-milli (zdt/to-instant (->date inst?))))

(defn date? [value]
  (instance? ZonedDateTime value))

(defn ->inst [inst]
  (cond
    (inst? inst) inst
    (instance? ZonedDateTime inst) (-> inst
                                       zdt/to-instant
                                       #?(:clj Date/from :cljs (js/Date.)))))

(defn relative [inst]
  (let [now (now)
        inst' (->date inst)]
    (cond
      (= (format now :date/system) (format inst' :date/system))
      "Today"

      (= (format now :date/system) (format (minus inst' 1 :days) :date/system))
      "Tomorrow"

      (not= (year now) (year inst'))
      (format inst' :date/view)

      (and (after? inst' now) (before? (minus inst' 6 :days) now))
      (str "This " (string/capitalize (name (day-of-week inst'))))

      (and (after? inst' now) (before? (minus inst' 13 :days) now))
      (str "Next " (string/capitalize (name (day-of-week inst'))))

      :else
      (format inst' :date.no-year/view))))
