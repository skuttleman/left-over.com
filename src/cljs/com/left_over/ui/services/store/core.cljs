(ns com.left-over.ui.services.store.core
  (:require [com.ben-allred.collaj.core :as collaj]
            [com.left-over.ui.services.store.reducers :as reducers]
            [reagent.core :as r]))

(defonce ^:private store (collaj/create-custom-store r/atom
                                                     reducers/reducer))

(def get-state (:get-state store))

(def dispatch (:dispatch store))
