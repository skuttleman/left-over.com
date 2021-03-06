(ns com.left-over.ui.services.store.actions
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.ui.services.env :as env]
    [com.left-over.shared.services.http :as http]
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
       (-> (http/with-client http/get url req)
           (v/then (partial conj [success])
                   (partial conj [failure]))
           (v/then dispatch))))))

(def fetch-photos
  (fetch* (nav/aws-for :aws/images) :photos {:headers {:x-api-key (env/get :aws-api-key)}}))

(def fetch-videos
  (fetch* (nav/aws-for :aws/videos) :videos {:headers {:x-api-key (env/get :aws-api-key)}}))

(def fetch-shows
  (fetch* (nav/aws-for :aws/shows) :shows {:headers {:x-api-key (env/get :aws-api-key)}}))

(def fetch-songs
  (fetch* (nav/aws-for :aws/songs) :songs {:headers {:x-api-key (env/get :aws-api-key)}}))

(defn select-song [song]
  [:song/select song])

(defn update-percentage [percentage]
  [:song/update-percentage (if (js/isNaN percentage) 0 percentage)])
