(ns com.left-over.ui.admin.services.store.actions
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.shared.services.http :as http]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.ui.services.forms.standard :as form.std]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.actions :as actions]))

(def ^:private toast-msgs
  {:auth/failed [:error (constantly [:div "Your login attempt failed. Please try again. If the problem persists, contact support."])]})

(def fetch-auth-info
  (actions/fetch* (nav/aws-for :aws/info) :auth.info {:token? true}))

(def fetch-locations
  (actions/fetch* (nav/aws-for :aws.admin/locations) :locations {:token? true}))

(def fetch-shows
  (actions/fetch* (nav/aws-for :aws.admin/shows) :shows {:token? true}))

(defn merge-calendar!
  ([event]
   (http/with-client merge-calendar! event))
  ([client _]
   (http/post client (nav/aws-for :aws.admin/calendar.merge) {:token? true})))

(def fetch-unconfirmed
  (actions/fetch* (nav/aws-for :aws.admin/calendar.merge) :calendar {:token? true}))

(defn fetch-show [show-id]
  (actions/fetch* (nav/aws-for :aws.admin/show {:route-params {:show-id show-id}}) :show {:token? true}))

(defn create-show [show]
  (fn -create-show
    ([event]
     (http/with-client -create-show event))
    ([client _]
     (http/post client (nav/aws-for :aws.admin/shows) {:body show :token? true}))))

(defn update-show [show-id show]
  (fn -update-show
    ([event]
     (http/with-client -update-show event))
    ([client _]
     (http/put client (nav/aws-for :aws.admin/show {:route-params {:show-id show-id}}) {:body show :token? true}))))

(defn delete-shows [show-ids]
  (fn -delete-shows
    ([event]
     (http/with-client -delete-shows event))
    ([client _]
     (http/delete client (nav/aws-for :aws.admin/shows) {:body {:show-ids show-ids} :token? true}))))

(defn create-location [location]
  (fn -create-location
    ([event]
     (http/with-client -create-location event))
    ([client _]
     (http/post client (nav/aws-for :aws.admin/locations) {:body location :token? true}))))

(defn update-location [location-id location]
  (fn -update-location
    ([event]
     (http/with-client -update-location event))
    ([client _]
     (http/put client (nav/aws-for :aws.admin/location {:route-params {:location-id location-id}}) {:body location :token? true}))))

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

(defn save-location [location-id show-form location]
  (fn [[dispatch]]
    (-> (if location-id
          (update-location location-id location)
          (create-location location))
        dispatch
        (act-or-toast (all fetch-locations
                           hide-modal
                           [:search/set nil]))
        dispatch
        (v/then (comp (partial swap! show-form assoc :location-id) :id)))))

(defn remove-shows [show-ids]
  (fn [[dispatch]]
    (let [[msg title] (if (= 1 (count show-ids))
                        ["Are you sure you want to remove this event?" "Remove Event"]
                        ["Are you sure you want to remove these events?" "Remove Events"])]
      (dispatch (show-modal msg
                            title
                            [:button.button.is-danger
                             {:on-click (fn [_]
                                          (-> show-ids
                                              delete-shows
                                              dispatch
                                              (act-or-toast fetch-unconfirmed)
                                              dispatch))}
                             "Remove"]
                            [:button.button "Cancel"])))))

(defn save-show [{show-id :id :as show}]
  (fn [[dispatch]]
    (let [show' (assoc show :confirmed? true)]
      (-> (if show-id
            (update-show show-id show')
            (create-show show'))
          dispatch
          (act-or-toast (navigate (if (or (not show-id) (:confirmed? show))
                                    :ui.admin/main
                                    :ui.admin/calendar)))
          dispatch))))

(defn delete-show [show-id]
  (fn [[dispatch]]
    (dispatch (show-modal "Are you sure you want to delete this show?"
                          "Delete Show"
                          [:button.button.is-danger
                           {:on-click (fn [_]
                                        (-> [show-id]
                                            delete-shows
                                            dispatch
                                            (act-or-toast fetch-shows)
                                            dispatch))}
                           "Delete"]
                          [:button.button "Cancel"]))))
