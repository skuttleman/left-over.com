(ns com.left-over.ui.moment
  (:require
    cljsjs.moment
    cljsjs.moment-timezone
    [clojure.string :as string]))

(def ^:private formats
  {:datetime/view     "ddd, MMM D, YYYY h:mm A"
   :datetime/local    "YYYY-MM-DDTHH:mm"
   :datetime/iso      "YYYY-MM-DDTHH:mm:ss.SSSZ"
   :date/system       "YYYY-MM-DD"
   :date/view         "ddd, MMM D, YYYY"
   :date.no-year/view "ddd, MMM D"
   :datetime/fs       "YYYYMMDDHHmmss"
   :date/year         "YYYY"
   :date/day          "D"
   :date/month        "MMM"
   :time/view         "h:mm A"})

(defn format
  ([inst]
   (format inst :datetime/view))
  ([inst fmt]
   (format inst fmt (.guess js/moment.tz)))
  ([inst fmt tz]
   (-> inst
       js/moment
       (.tz tz)
       (.format (formats fmt fmt)))))

(defn stringify [inst]
  (-> inst
      (format :datetime/iso "UTC")
      (string/replace "+00:00" "-00:00")))

(defn parse [s]
  (.toDate (js/moment s)))

(defn plus [inst amt interval]
  (let [mo (js/moment inst)]
    (.add mo amt (name interval))
    (.toDate mo)))

(defn minus [inst amt interval]
  (plus inst (* -1 amt) interval))

(defn with [inst amt interval]
  (let [mo (js/moment inst)]
    (case interval
      :year (.year mo amt)
      :month (.month mo amt)
      :day (.day mo amt)
      :hour (.hour mo amt)
      :minute (.minute mo amt)
      :second (.second mo amt))
    (.toDate mo)))

(defn year [inst]
  (.year (js/moment inst)))

(defn day-of-week
  ([inst]
   (day-of-week inst (.guess js/moment.tz)))
  ([inst tz]
   (keyword (string/lower-case (format inst "dddd" tz)))))

(defn after? [inst-1 inst-2]
  (.isAfter (js/moment inst-1) (js/moment inst-2)))

(defn before? [inst-1 inst-2]
  (.isBefore (js/moment inst-1) (js/moment inst-2)))

(defn now []
  (js/Date.))

(defn inst->ms [inst]
  (.getTime inst))

(defn date? [v]
  (instance? js/Date v))

(defn relative [inst]
  (let [now (js/moment (now))
        mo (js/moment inst)]
    (cond
      (= (format now :date/system) (format mo :date/system))
      "Today"

      (= (format now :date/system) (format (minus mo 1 :days) :date/system))
      "Tomorrow"

      (not= (year now) (year mo))
      (format mo :date/view)

      (and (after? mo now) (before? (minus mo 6 :days) now))
      (str "This " (string/capitalize (name (day-of-week mo))))

      (and (after? mo now) (before? (minus mo 13 :days) now))
      (str "Next " (string/capitalize (name (day-of-week mo))))

      :else
      (format mo :date.no-year/view))))
