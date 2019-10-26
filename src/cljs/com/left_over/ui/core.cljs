(ns com.left-over.ui.core
  (:require
    [com.left-over.ui.services.navigation :as nav]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.about :as about]
    [com.left-over.ui.views.contact :as contact]
    [com.left-over.ui.views.main :as main]
    [com.left-over.ui.views.navbar :as navbar]
    [com.left-over.ui.views.news :as news]
    [com.left-over.ui.views.photos :as photos]
    [com.left-over.ui.views.shows :as shows]
    [reagent.core :as r]
    com.left-over.ui.services.navigation
    ))

(enable-console-print!)

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
  {:ui/main       main/root
   :ui/news       news/root
   :ui/about      about/root
   :ui/shows      shows/root
   :ui/photos     photos/root
   :ui/contact    contact/root
   :nav/not-found not-found})

(defn ^:private app []
  (let [state (store/get-state)
        handler (get-in state [:page :handler])
        component (components handler not-found)]
    [:div.columns {:style {:min-height "100vh" :margin-top "0"}}
     [:div.column.is-variable.is-0-mobile {:style {:padding "0"}}]
     [:div.column.is-variable {:style {:height  "100vh"
                                       :padding 0}}
      [:div.rows {:style {:height         "100%"
                          :display        :flex
                          :flex-direction :column}}
       [:div.row
        [navbar/logo]]
       (when (not= :ui/main handler)
         [:div.row
          [navbar/nav-bar state]])
       [:div.row {:style (handler->style handler)}
        [:div {:style {:margin "16px"}}
         [component state]]]]]
     [:div.column.is-variable.is-0-mobile {:style {:padding "0"}}]]))

(defn ^:export mount! []
  (r/render-component [app]
                      (.getElementById js/document "app")))
