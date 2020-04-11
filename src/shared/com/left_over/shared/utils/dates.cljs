(ns com.left-over.shared.utils.dates
  (:refer-clojure :exclude [format])
  (:require
    [cljc.java-time.day-of-week :as dow]
    [cljc.java-time.format.date-time-formatter :as dtf]
    [cljc.java-time.instant :as inst]
    [cljc.java-time.local-date :as ld]
    [cljc.java-time.local-time :as lt]
    [cljc.java-time.zone-id :as zi]
    [cljc.java-time.zone-offset :as zo]
    [cljc.java-time.zoned-date-time :as zdt]
    [clojure.string :as string]
    [java.time :refer [ZonedDateTime]]
    [tick.format :as tf]
    tick.locale-en-us))

(def utc zo/utc)

(def ^:private formats
  {:datetime/view     "EEE, MMM d, yyyy h:mm a"
   :datetime/local    "yyyy-MM-dd'T'HH:mm"
   :datetime/iso      "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
   :date/system       "yyyy-MM-dd"
   :date/view         "EEE, MMM d, yyyy"
   :date.no-year/view "EEE, MMM d"
   :datetime/fs       "yyyyMMddHHmmss"
   :date/year         "yyyy"
   :date/day          "d"
   :date/month        "MMM"
   :time/view         "h:mm a"})

(defn ^:private ->zdt [v]
  (cond
    (instance? ZonedDateTime v) v
    (string? v) (zdt/parse v)
    (inst? v) (zdt/of-instant (inst/of-epoch-milli (.getTime v)) utc)))

(defn ^:private ->inst [v]
  (cond
    (instance? ZonedDateTime v)
    (js/Date. (inst/to-epoch-milli (zdt/to-instant v)))

    (and (string? v) (re-matches #"\d{4}-\d{2}-\d{2}" v))
    (-> v
        ld/parse
        (zdt/of lt/midnight (zi/system-default))
        zdt/to-instant
        inst/to-epoch-milli
        js/Date.)

    :else
    (js/Date. v)))

(defn format
  ([inst]
   (format inst :datetime/view))
  ([inst fmt]
   (format inst fmt (zo/system-default)))
  ([inst fmt tz]
   (let [inst' (zdt/with-zone-same-instant (->zdt inst) tz)]
     (-> fmt
         (formats fmt)
         tf/formatter
         (dtf/format inst')))))

(defn stringify [inst]
  (-> inst
      (format :datetime/iso utc)
      (string/replace "+0000" "-00:00")))

(defn parse [s]
  (->inst s))

(defn plus [inst amt interval]
  (let [zdt (->zdt inst)]
    (->inst (case interval
              :years (zdt/plus-years zdt amt)
              :months (zdt/plus-months zdt amt)
              :weeks (zdt/plus-weeks zdt amt)
              :days (zdt/plus-days zdt amt)
              :hours (zdt/plus-hours zdt amt)
              :minutes (zdt/plus-minutes zdt amt)
              :seconds (zdt/plus-seconds zdt amt)
              zdt))))

(defn minus [inst amt interval]
  (plus inst (* -1 amt) interval))

(defn with [inst amt interval]
  (let [zdt (->zdt inst)]
    (->inst (case interval
              :year (zdt/with-year zdt amt)
              :month (zdt/with-month zdt amt)
              :day (zdt/with-day-of-month zdt amt)
              :hour (zdt/with-hour zdt amt)
              :minute (zdt/with-minute zdt amt)
              :second (zdt/with-second zdt amt)
              zdt))))

(defn year [inst]
  (format inst :date/year))

(defn month [inst]
  (format inst :date/month))

(defn day [inst]
  (format inst :date/day))

(defn day-of-week [inst]
  (-> inst
      ->zdt
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
      ->zdt
      (zdt/is-after (->zdt date-2))))

(defn before? [date-1 date-2]
  (-> date-1
      ->zdt
      (zdt/is-before (->zdt date-2))))

(defn now []
  (js/Date.))

(defn inst->ms [inst]
  (.getTime inst))

(defn date? [v]
  (instance? js/Date v))

(defn relative [inst]
  (let [now (->zdt (now))
        inst' (->zdt inst)]
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
