(ns com.left-over.ui.admin.services.store.actions
  (:require
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.navigation :as nav]))

(def fetch-auth-info
  (actions/fetch* (nav/api-for :auth/info) :auth.info))
