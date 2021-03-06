(ns com.left-over.ui.main
  (:require
    [com.left-over.shared.utils.logging :as log]
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.about :as about]
    [com.left-over.ui.views.contact :as contact]
    [com.left-over.ui.views.main :as main]
    [com.left-over.ui.views.music :as music]
    [com.left-over.ui.views.navbar :as navbar]
    [com.left-over.ui.views.photos :as photos]
    [com.left-over.ui.views.shows :as shows]
    [com.left-over.ui.views.videos :as videos]
    [reagent.core :as r]))

(enable-console-print!)
(aset js/window "Buffer" #js {:isBuffer (constantly false)})

(defn not-found [_]
  [:div
   [:p "page not found"]
   [:p
    "go "
    [:a {:href (nav/path-for :ui/main)} "home"]]])

(def ^:private handler->style
  {:ui/main {:align-items     :center
             :display         :flex
             :flex-grow       2
             :justify-content :center}})

(def ^:private components
  {:ui/main    main/root
   :ui/about   about/root
   :ui/shows   shows/root
   :ui/photos  photos/root
   :ui/music   music/root
   :ui/videos  videos/root
   :ui/contact contact/root})

(defn ^:private app []
  (let [state (store/get-state)
        handler (get-in state [:page :handler])
        component (components handler not-found)]
    [:div.columns {:style {:max-width "100vw"
                           :margin    0}}
     [:div.column.is-variable.is-0-mobile {:style {:padding 0}}]
     [:div.column.is-variable {:style {:padding 0}}
      [:div.main
       [:div
        [navbar/logo false]]
       (when (not= :ui/main handler)
         [:div
          [navbar/nav-bar state]])
       [:div {:style (handler->style handler)}
        [:div {:style {:margin "16px"}}
         [component state]]]]
      [:div.has-text-centered
       {:style {:color :gray}}
       "View our "
       [:a {:href "/privacy" :target "_blank"} "privacy policy"]]]
     [:div.column.is-variable.is-0-mobile {:style {:padding 0}}]]))

(defn ^:export mount! []
  (r/render-component [app] (.getElementById js/document "app")))
