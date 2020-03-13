(ns com.left-over.ui.core
  (:require
    [com.left-over.ui.services.env :as env]
    [com.left-over.ui.admin :as ui.admin]
    [com.left-over.ui.main :as ui.main]))

(enable-console-print!)

(defn ^:export mount! []
  (if (env/get :admin?)
    (ui.admin/mount!)
    (ui.main/mount!)))
