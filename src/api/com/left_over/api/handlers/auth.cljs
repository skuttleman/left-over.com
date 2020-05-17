(ns com.left-over.api.handlers.auth
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.core :as core]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.env :as env]
    [com.left-over.api.services.google :as google]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.shared.services.http :as http]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.shared.services.protocols :as p]))

(defn ^:private fetch-access-token [oauth {:keys [code]}]
  (p/token oauth {:grant_type   "authorization_code"
                  :code         code
                  :redirect_uri (env/get :oauth-redirect-uri)}))

(defn ^:private token->user [conn oauth token-info]
  (if-let [access-token (:access_token token-info)]
    (v/await [response (-> oauth
                           (p/profile access-token)
                           (v/peek #(log/info "Fetched User PROFILE from "
                                              (env/get :oauth-token-info-uri)
                                              ": "
                                              ;; TODO: (pop %) after setting up John
                                              %)))
              user (users/find-by-email conn (:email response))]
      (when user
        (v/and (users/merge-token-info conn (:id user) token-info)
               (select-keys user #{:email :first-name :last-name :id}))))
    (v/reject "Missing token")))

(defn handle-callback [conn oauth params]
  (v/await [redirect-uri (:redirect-uri (edn/parse (:state params)))
            jwt (v/attempt (-> (if redirect-uri
                                 (fetch-access-token oauth params)
                                 (v/reject "missing redirect-uri"))
                               (v/peek #(log/info "Fetched Auth TOKEN from" (env/get :oauth-token-uri) ": " (pop %)))
                               (v/then-> (->> (token->user conn oauth)) (some-> jwt/encode)))
                           (catch error
                             (log/error "ERROR authenticating" error)))]
    (cond
      jwt (str redirect-uri "?token=" jwt)
      redirect-uri (str redirect-uri "?token-msg-id=auth/failed")
      :else (v/reject))))

(defn ^:private redirect-to [url]
  (with-meta {}
             {:status  302
              :headers {:Location url}}))

(defmulti ^:private handler* :resource)

(defmethod ^:private handler* "/auth/callback"
  [{:keys [queryStringParameters] :as event}]
  (v/attempt (v/then (repos/transact (fn [conn]
                                       (http/with-client google/with-oauth
                                                         (partial handle-callback conn)
                                                         queryStringParameters)))
                     redirect-to)
             (catch _
               (throw (ex-info "cannot redirect" {:event event})))))

(defmethod ^:private handler* "/auth/login"
  [event]
  (v/resolve (redirect-to (http/with-client google/with-oauth
                                            p/redirect-uri
                                            (:queryStringParameters event)))))

(defmethod ^:private handler* "/auth/info"
  [event]
  (v/resolve (or (:user event)
                 ^{:status 401} {:message "unauthorized"})))

(def handler (core/with-event (core/with-user handler*)))

(set! (.-exports js/module) #js {:handler handler})
