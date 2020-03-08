(ns com.left-over.ui.views.navbar
  (:require
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.views.components :as components]
    [reagent.core :as r]))

(defn logo [admin?]
  [:a.navbar-item
   {:href  (nav/path-for (if admin? :ui.admin/main :ui/main))
    :style {:padding 0}}
   [:img {:src    (nav/s3-for :s3/image {:route-params {:image "logo.jpg"}})
          :width  "100%"
          :height "auto"
          :style  {:max-width  "100vw"
                   :max-height "unset"}}]])

(defn nav-item [{:keys [page]} pg icon]
  (let [page-str (name pg)
        active? (= pg (:handler page))]
    [:li
     {:class [(when active? "is-active")]}
     [:a.navbar-item
      {:href (nav/path-for pg)}
      [components/icon icon]
      [:pre {:style {:padding 0 :font-size "1rem" :background-color :transparent}} "  "]
      [:span {:class [(when-not active? "is-hidden-mobile")]} page-str]]]))

(defn nav-bar [state]
  [:nav.tabs.main-navigation
   [:ul
    (for [[pg icon] [[:ui/about :info-circle]
                     [:ui/shows :music]
                     [:ui/photos :images]
                     [:ui/videos :video]
                     [:ui/contact :link]]]
      ^{:key pg} [nav-item state pg icon])]
   [:div.navbar-end]])
