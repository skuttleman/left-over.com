(ns com.left-over.ui.admin.services.store.actions
  (:require
    [cljs.core.async :as async]
    [com.left-over.common.utils.dates :as dates]
    [com.left-over.ui.services.forms.standard :as form.std]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.common.services.http :as http]
    [com.ben-allred.vow.core :as v]))

(def ^:private toast-msgs
  {:auth/failed [:error (constantly [:div "Your login attempt failed. Please check your email and password and try again."])]})

(def fetch-auth-info
  (actions/fetch* (nav/api-for :auth/info) :auth.info))

(def fetch-locations
  (actions/fetch* (nav/api-for :api.admin/locations) :locations))

(def fetch-shows
  (actions/fetch* (nav/api-for :api.admin/shows) :shows))

(defn fetch-show [show-id]
  (actions/fetch* (nav/api-for :api.admin/show {:route-params {:show-id show-id}}) :show))

(defn create-show [show]
  (fn [_]
    (http/post (nav/api-for :api.admin/shows) {:body show})))

(defn update-show [show-id show]
  (fn [_]
    (http/put (nav/api-for :api.admin/show {:route-params {:show-id show-id}}) {:body show})))

(defn delete-show [show-id]
  (fn [_]
    (http/delete (nav/api-for :api.admin/show {:route-params {:show-id show-id}}))))

(defn create-location [location]
  (fn [_]
    (http/post (nav/api-for :api.admin/locations) {:body location})))

(defn update-location [location-id location]
  (fn [_]
    (http/put (nav/api-for :api.admin/location {:route-params {:location-id location-id}}) {:body location})))

(defn create-form
  ([model]
   (create-form model (constantly nil)))
  ([model validator]
   (let [internal-id (gensym)
         external-id (gensym)]
     (fn [[dispatch]]
       (form.std/create model validator dispatch internal-id external-id)
       external-id))))

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

(defn navigate
  ([page]
   (navigate page nil))
  ([page params]
   (fn [_]
     (nav/navigate! page params))))

(defn all [& actions]
  (fn [[dispatch]]
    (run! dispatch actions)))

(defn act-or-toast [promise action]
  (fn [[dispatch]]
    (v/peek promise
            (fn [_] (dispatch action))
            (fn [_] (dispatch (toast! :error "Something went wrong"))))))

(defn toast-by-id! [toast-msg-id params]
  (let [[level body-fn] (get toast-msgs toast-msg-id)]
    (toast! level (body-fn params))))

(defn show-modal [content & [title & actions]]
  (fn [[dispatch]]
    (dispatch [:modal/mount content title actions])
    (js/setTimeout #(dispatch [:modal/show]) 1)))

(def hide-modal
  (fn [[dispatch]]
    (dispatch [:modal/hide])
    (js/setTimeout #(dispatch [:modal/unmount]) 333)))