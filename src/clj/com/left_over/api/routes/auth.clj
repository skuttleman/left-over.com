(ns com.left-over.api.routes.auth
  (:require
    [clj-oauth2.client :as oauth2]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.env :as env]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.common.services.http :as http]
    [com.left-over.common.utils.edn :as edn]
    [com.left-over.common.utils.json :as json]
    [com.left-over.common.utils.maps :as maps]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [ring.util.response :as resp]
    [com.left-over.common.utils.logging :as log]))

(def oauth-config
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

(defn token->user [token db]
  (-> (http/get (env/get :oauth-token-info-uri) {:query-params {:access_token token}})
      (v/then-> :email (->> (users/find-by-email db)))
      deref))

(defroutes routes
  (GET "/callback" {:keys [db query-params]}
    (let [{redirect-uri :state :as qp} (maps/map-kv keyword identity query-params)
          [status user] (-> (oauth2/get-access-token oauth-config qp)
                            :access-token
                            (token->user db))]
      (if (and (= status :success) user)
        (resp/redirect (str redirect-uri "?token=" (jwt/encode user)))
        (resp/redirect (str redirect-uri "?toast-msg-id=auth/failed")))))
  (GET "/info" {:auth/keys [user]}
    (if user
      {:status 200
       :body   user}
      {:status 401
       :body   {:message "unauthorized"}}))
  (GET "/login" {:keys [query-params]}
    (resp/redirect (:uri (oauth2/make-auth-request oauth-config (get query-params "redirect-uri"))))))
