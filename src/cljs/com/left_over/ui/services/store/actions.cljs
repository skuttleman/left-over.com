(ns com.left-over.ui.services.store.actions
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.env :as env]
    [com.left-over.common.services.http :as http]
    [com.left-over.ui.services.navigation :as nav]))

(defn fetch*
  ([url action-ns]
   (fetch* url action-ns nil))
  ([url action-ns req]
   (let [[request success failure] (map keyword
                                        (repeat (name action-ns))
                                        ["request" "success" "failure"])]
     (fn [[dispatch]]
       (dispatch [request])
       (-> url
           (http/get req)
           (v/then (partial conj [success])
                   (partial conj [failure]))
           (v/then dispatch))))))

(def fetch-photos
  (fetch* (env/get :dropbox-images-uri) :photos {:headers {:x-api-key (env/get :aws-api-key)}}))

(def fetch-shows
  (fetch* (nav/api-for :api/shows) :shows))
