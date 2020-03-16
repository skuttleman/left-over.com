(ns com.left-over.ui.admin.views.auth
  (:require
    [com.left-over.shared.utils.uri :as uri]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.views.components :as components]))

(defn login [_state]
  [:div.has-text-centered
   [:h1.title "Admin Portal"]
   [:a.button.is-link {:href (->> (nav/ui-for :ui.admin/main)
                                  uri/url-encode
                                  (hash-map :redirect-uri)
                                  (hash-map :query-params)
                                  (nav/aws-for :aws/login))}
    [components/icon {:icon-style :brand} :google-plus-g]
    [:span {:style {:margin-left "8px"}} "Login with Google"]]])

(defn logout []
  [:button.button.is-link
   {:on-click (fn [_]
                (nav/logout!))}
   "Logout"])
