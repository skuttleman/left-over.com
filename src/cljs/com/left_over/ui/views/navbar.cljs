(ns com.left-over.ui.views.navbar
  (:require
    [com.left-over.ui.services.navigation :as nav]
    [reagent.core :as r]))

(defn logo []
  [:a.navbar-item {:href (nav/path-for :ui/main)
                   :style {:padding 0}}
   [:img {:src    "/images/logo.jpg"
          :width  "100%"
          :height "auto"
          :style  {:max-width  "703px"
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
        (for [pg [:ui/news :ui/about :ui/shows :ui/photos :ui/contact]]
          ^{:key pg} [nav-item state is-active? pg])]
       [:div.navbar-end]])))
