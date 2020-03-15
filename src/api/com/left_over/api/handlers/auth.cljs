(ns com.left-over.api.handlers.auth
  (:require
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.common.utils.uri :as uri]
    [com.left-over.api.core :as core]
    [com.left-over.common.services.http :as http]
    [clojure.string :as string]
    [com.left-over.api.services.env :as env]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.logging :as log]
    [com.left-over.common.services.db.repositories.core :as repos]
    [com.left-over.common.services.db.models.users :as users]
    [com.left-over.api.services.jwt :as jwt]))

(def ^:private oauth-config
  {:redirect-uri       (env/get :oauth-redirect-uri)
   :client-id          (env/get :oauth-client-id)
   :client-secret      (env/get :oauth-client-secret)
   :scope              (edn/parse (env/get :oauth-scopes))
   :authorization-uri  (env/get :oauth-authorization-uri)
   :access-token-uri   (env/get :oauth-token-uri)
   :access-query-param :access_token
   :grant-type         "authorization_code"
   :access-type        "online"
   :approval_prompt    ""})

(defn ^:private fetch-access-token [config {:keys [code]}]
  (let [{:keys [access-token-uri client-id client-secret grant-type redirect-uri]} config
        request {:content-type "application/x-www-form-urlencoded"
                 :body         (uri/form-url-encode {:grant_type    grant-type
                                                     :code          code
                                                     :redirect_uri  redirect-uri
                                                     :client_id     client-id
                                                     :client_secret client-secret})}]
    (http/post access-token-uri request)))

(defn ^:private oauth-redirect-uri
  [{:keys [authorization-uri client-id redirect-uri scope access-type]} & [state]]
  (-> authorization-uri
      uri/parse
      (assoc :query (cond-> {:client_id     client-id
                             :redirect_uri  redirect-uri
                             :response_type "code"}
                      state (assoc :state state)
                      access-type (assoc :access_type access-type)
                      scope (assoc :scope (string/join " " scope))))
      uri/stringify))

(defn ^:private token->user [token]
  (when token
    (repos/transact (fn [conn]
                      (-> (env/get :oauth-token-info-uri)
                          (http/get {:query-params {:access_token token}
                                     :json?        true})
                          (v/peek #(log/info "Fetched User PROFILE from " (env/get :oauth-token-info-uri) ": " %))
                          (v/then-> :email (->> (users/find-by-email conn))))))))

(defn ^:private redirect-to [url]
  (with-meta {}
             {:status  302
              :headers {:Location url}}))

(defmulti ^:private handler* :resource)

(defmethod ^:private handler* "/auth/callback"
  [{:keys [queryStringParameters] :as event}]
  (let [redirect-uri (:redirect-uri (edn/parse (:state queryStringParameters)))]
    (-> (if redirect-uri
          (fetch-access-token oauth-config queryStringParameters)
          (v/reject "missing redirect-uri"))
        (v/peek #(log/info "Fetched Auth TOKEN from " (env/get :oauth-token-uri) ": " %))
        (v/then :access_token)
        (v/then token->user)
        (v/then-> (some-> jwt/encode))
        (v/catch (constantly nil))
        (v/then (fn [jwt]
                  (cond
                    jwt (redirect-to (str redirect-uri "?token=" jwt))
                    redirect-uri (redirect-to (str redirect-uri "?token-msg-id=auth/failed"))
                    :else (throw (ex-info "cannot redirect" {:event event}))))))))

(defmethod ^:private handler* "/auth/login"
  [event]
  (v/resolve (redirect-to (oauth-redirect-uri oauth-config (:queryStringParameters event)))))

(defmethod ^:private handler* "/auth/info"
  [event]
  (v/resolve (or (:user event)
                 ^{:status 401} {:message "unauthorized"})))

(def handler (core/with-event (core/with-user handler*)))

(set! (.-exports js/module) #js {:handler handler})
