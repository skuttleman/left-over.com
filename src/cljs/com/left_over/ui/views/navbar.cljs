(ns com.left-over.ui.views.navbar
  (:require
    [com.left-over.common.services.env :as env]
    [com.left-over.ui.services.navigation :as nav]
    [reagent.core :as r]))

(defn logo [admin?]
  [:a.navbar-item
   {:href  (nav/path-for (if admin? :ui.admin/main :ui/main))
    :style {:padding 0}}
   [:img {:src    (nav/api-for :api/image {:route-params {:image "logo.jpg"}})
          :width  "100%"
          :height "auto"
          :style  {:max-width  "100vw"
                   :max-height "unset"}}]])

(defn nav-item [{:keys [page]} is-active? pg]
  (let [page-str (name pg)]
    [:li
     {:class [(when (= pg (:handler page))
                "is-active")]}
     [:a.navbar-item
      {:href     (nav/path-for pg)
       :on-click (fn [_] (reset! is-active? false))}
      page-str]]))

(defn nav-bar [_state]
  (let [is-active? (r/atom false)]
    (fn [state]
      [:nav.tabs.main-navigation
       {:class [(when @is-active? "is-active")]}
       [:ul
        (for [pg [:ui/about :ui/shows :ui/photos :ui/contact]]
          ^{:key pg} [nav-item state is-active? pg])]
       [:div.navbar-end]])))
