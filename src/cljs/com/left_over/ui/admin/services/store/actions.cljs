(ns com.left-over.ui.admin.services.store.actions
  (:require
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.forms.standard :as form.std]
    [com.left-over.ui.services.navigation :as nav]
    [cljs.core.async :as async]
    [com.left-over.common.utils.dates :as dates]))

(def ^:private toast-msgs
  {:auth/failed [:error (constantly [:div "Your login attempt failed. Please check your email and password and try again."])]})

(def fetch-auth-info
  (actions/fetch* (nav/api-for :auth/info) :auth.info))

(def fetch-locations
  (actions/fetch* (nav/api-for :api.admin/locations) :locations))

(def fetch-shows
  (actions/fetch* (nav/api-for :api.admin/shows) :shows))

(defn create-form [model]
  (let [internal-id (gensym)
        external-id (gensym)]
    (fn [[dispatch]]
      (form.std/create model (constantly nil) dispatch internal-id external-id)
      external-id)))

(defn remove-toast! [toast-id]
  (fn [[dispatch]]
    (dispatch [:toast/hide {:id toast-id}])
    (async/go
      (async/<! (async/timeout 500))
      (dispatch [:toast/remove {:id toast-id}]))
    nil))

(defn toast! [level body]
  (fn [[dispatch]]
    (let [toast-id (dates/inst->ms (dates/now))]
      (->> {:id    toast-id
            :level level
            :body  (delay (async/go
                            (dispatch [:toast/show {:id toast-id}])
                            (async/<! (async/timeout 6000))
                            (dispatch (remove-toast! toast-id)))
                          body)}
           (conj [:toast/add])
           dispatch))
    nil))

(defn toast-by-id! [toast-msg-id params]
  (let [[level body-fn] (get toast-msgs toast-msg-id)]
    (toast! level (body-fn params))))
