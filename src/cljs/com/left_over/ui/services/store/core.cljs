(ns com.left-over.ui.services.store.core
  (:require [com.ben-allred.collaj.core :as collaj]
            [com.ben-allred.collaj.enhancers :as collaj.enhancers]
            [com.left-over.ui.services.store.reducers :as reducers]
            [reagent.core :as r]))

(defonce ^:private store (collaj/create-custom-store r/atom
                                                     reducers/reducer
                                                     collaj.enhancers/with-fn-dispatch))

(def get-state (:get-state store))

(def dispatch (:dispatch store))
