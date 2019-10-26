(ns com.left-over.ui.services.store.actions
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.common.services.http :as http]
    [com.left-over.common.services.env :as env]
    [com.left-over.ui.services.navigation :as nav]))

(def fetch-photos
  (fn [[dispatch]]
    (dispatch [:photos/request])
    (-> (http/get (str (env/get :api-base-url) (nav/path-for :api/photos)))
        (v/then (partial conj [:photos/success])
                (partial conj [:photos/failure]))
        (v/then dispatch))))
