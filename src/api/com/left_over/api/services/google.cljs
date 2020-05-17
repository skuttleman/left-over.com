(ns com.left-over.api.services.google
  (:require
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.env :as env]
    [com.left-over.shared.services.http :as http]
    [com.left-over.shared.services.protocols :as p]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.shared.utils.strings :as strings]
    [com.left-over.shared.utils.uri :as uri]
    fs))

(def ^:private oauth-config
  {:client-id     (env/get :oauth-client-id)
   :client-secret (env/get :oauth-client-secret)
   :scope         (edn/parse (env/get :oauth-scopes))})


(defn ^:private refresh [oauth token-info]
  (when-let [refresh-token (:refresh_token token-info)]
    (p/token oauth {:grant_type    "refresh_token"
                    :refresh_token refresh-token})))

(defn ^:private handle-refresh [conn oauth user-id token-info token->request]
  (v/await [new-token-info (some->> token-info (refresh oauth))]
    (users/merge-token-info conn user-id new-token-info)
    (token->request (:access_token new-token-info))))

(defn ^:private expired? [token-info]
  (try
    (<= (:expires_at token-info) (dates/plus (dates/now) 1 :seconds))
    (catch :default _
      false)))

(defn ^:private with-token-refresh [conn oauth user-id token->request]
  (v/await [{:keys [token-info]} (users/find-by-id conn user-id)]
    (v/attempt (if (expired? token-info)
                 (v/reject "TOKEN expired, skipping first request")
                 (token->request (:access_token token-info)))
               (catch err
                 (log/debug "Refreshing auth TOKEN:" err)
                 (handle-refresh conn oauth user-id token-info token->request)))))

(defn ^:private fetch-events* [conn client oauth user-id calendar-id]
  (with-token-refresh conn
                      oauth
                      user-id
                      (fn [access-token]
                        (let [now (dates/now)
                              next-year (dates/plus now 1 :years)]
                          (http/get client
                                    (strings/format (env/get :google-calendar-events) calendar-id)
                                    {:query-params {:showDeleted  false
                                                    :singleEvents true
                                                    :timeMin      (dates/stringify now)
                                                    :timeMax      (dates/stringify next-year)}
                                     :headers      {"authorization" (str "Bearer " access-token)}
                                     :json?        true})))))

(defn with-oauth [client f & args]
  (let [oauth (reify p/IOauthProvider
                (redirect-uri [_ state]
                  (let [{:keys [client-id scope]} oauth-config]
                    (-> (env/get :oauth-authorization-uri)
                        uri/parse
                        (assoc :query (cond-> {:client_id       client-id
                                               :redirect_uri    (env/get :oauth-redirect-uri)
                                               :response_type   "code"
                                               :access_type     "offline"
                                               :approval_prompt "force"}
                                        state (assoc :state state)
                                        scope (assoc :scope (string/join " " scope))))
                        uri/stringify)))
                (token [_ params]
                  (let [{:keys [client-id client-secret]} oauth-config
                        request {:headers {:content-type "application/x-www-form-urlencoded"}
                                 :body    (uri/form-url-encode (assoc params
                                                                      :client_id client-id
                                                                      :client_secret client-secret))
                                 :json?   true}]
                    (http/post client (env/get :oauth-token-uri) request)))
                (profile [_ access-token]
                  (http/get client
                            (env/get :oauth-token-info-uri)
                            {:query-params {:access_token access-token}
                             :json?        true})))]
    (apply f oauth args)))

(defn ->oauth [client]
  (with-oauth client identity))

(defn fetch-calendar-events [conn client oauth user-id]
  (v/then-> (fetch-events* conn client oauth user-id (env/get :google-calendar-id))
            :items
            (->> (map (fn [event]
                        (-> event
                            (select-keys #{:id :start :end :summary :description})
                            (assoc :show? (not (re-find #"(?i)(dnb|rehears)" (str (:summary event)))))))))))
