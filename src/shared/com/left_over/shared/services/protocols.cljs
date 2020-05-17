(ns com.left-over.shared.services.protocols)

(defprotocol IHttpClient
  (go [this method url request]))

(defprotocol IDbConnection
  (query [this sql params])
  (prepare [this query]))

(defprotocol IOauthProvider
  (redirect-uri [this state])
  (token [this params])
  (profile [this access-token]))
