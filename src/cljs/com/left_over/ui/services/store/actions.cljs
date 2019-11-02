(ns com.left-over.ui.services.store.actions
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.http :as http]
    [com.left-over.common.services.env :as env]
    [com.left-over.ui.services.navigation :as nav]))

(defn ^:private fetch* [url action-ns]
  (let [[request success failure] (map keyword
                                       (repeat (name action-ns))
                                       ["request" "success" "failure"])]
    (fn [[dispatch]]
      (dispatch [request])
      (-> url
          (http/get)
          (v/then (partial conj [success])
                  (partial conj [failure]))
          (v/then dispatch)))))

(def fetch-photos
  (fetch* (nav/api-for :api/photos) :photos))

(def fetch-past-shows
  (fetch* (nav/api-for :api/shows.past) :shows.past))

(def fetch-upcoming-shows
  (fetch* (nav/api-for :api/shows.upcoming) :shows.upcoming))
