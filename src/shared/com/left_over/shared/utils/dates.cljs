(ns com.left-over.shared.utils.dates
  (:require
    [cljs-time.core :as tt]
    [cljs-time.coerce :as tc]))

(defn stringify [inst]
  (.toISOString inst))

(defn parse [s]
  (tc/to-date (tc/to-date-time s)))

(defn plus [inst amt interval]
  (let [zdt (tc/to-date-time inst)]
    (-> interval
        (case
          :years (tt/plus zdt (tt/years amt))
          :months (tt/plus zdt (tt/months amt))
          :weeks (tt/plus zdt (tt/weeks amt))
          :days (tt/plus zdt (tt/days amt))
          :hours (tt/plus zdt (tt/hours amt))
          :minutes (tt/plus zdt (tt/minutes amt))
          :seconds (tt/plus zdt (tt/seconds amt))
          zdt)
        tc/to-date)))

(defn minus [inst amt interval]
  (plus inst (* -1 amt) interval))

(defn with [inst amt interval]
  (let [zdt (tc/to-date-time inst)]
    (case interval
      :year (.setYear zdt amt)
      :month (.setMonth zdt amt)
      :day (.setDay-of-month zdt amt)
      :hour (.setHour zdt amt)
      :minute (.setMinute zdt amt)
      :second (.setSecond zdt amt)
      nil)
    (tc/to-date zdt)))

(defn after? [inst-1 inst-2]
  (tt/after? (tc/to-date-time inst-1)
             (tc/to-date-time inst-2)))

(defn before? [inst-1 inst-2]
  (tt/before? (tc/to-date-time inst-1)
              (tc/to-date-time inst-2)))

(defn now []
  (js/Date.))

(defn inst->ms [inst]
  (.getTime inst))

(defn date? [v]
  (instance? js/Date v))
