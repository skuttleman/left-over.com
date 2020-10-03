(ns com.left-over.ui.views.music
  (:require
    [com.left-over.ui.services.store.actions :as actions]
    [com.left-over.ui.services.store.core :as store]
    [com.left-over.ui.views.components :as components]
    [reagent.core :as r]))

(defn ^:private wave-form [{:keys [audio height percentage width]} image-1 image-2]
  (when (and image-1 image-2)
    (let [position (-> percentage
                       (* width)
                       js/Math.round
                       (/ (/ width 100))
                       (min 100)
                       (max 0)
                       (str "%"))
          height-px (str height "px")
          width-px (str width "px")]
      [:div.wave-form {:style    {:background-image (str "url(" image-1 ")")
                                  :width            width-px
                                  :height           height-px}
                       :on-click (fn [event]
                                   (let [percentage (/ (-> event .-nativeEvent .-offsetX) width)]
                                     (store/dispatch (actions/update-percentage percentage))
                                     (aset audio "currentTime" (* percentage (.-duration audio)))))}
       [:div.percentage {:style {:background-image (str "url(" image-2 ")")
                                 :height           height-px
                                 :width            position}}]])))

(defn ^:private album-details [album]
  [:div
   [:img {:src (:image-url album)}]
   [:ul.media-links
    (for [media-link (:media-links album)]
      ^{:key (:name media-link)}
      [:li.media-link
       [:a {:href (:url media-link) :target :_blank}
        [:div.icon-container
         [components/icon {:icon-style :brand} (:icon media-link)]]
        (:name media-link)]])]])

(defn ^:private song-list [audio-ref selected-song albums]
  [:aside.menu
   (for [album albums]
      ^{:key (:id album)}
     [:div
      [:p.menu-label (:title album)]
      [:ul.menu-list.songs
       (for [song (:songs album)
             :let [selected? (= (dissoc selected-song :album) song)]]
         ^{:key (:id song)} [:li.song {:on-click (if selected?
                                                   (fn [_]
                                                     (when-let [audio @audio-ref]
                                                       (if (.-paused audio)
                                                         (.play audio)
                                                         (.pause audio))))
                                                   (fn [_]
                                                     (store/dispatch (actions/select-song (assoc song :album (dissoc album :songs))))))}
                             [:a {:href "#" :class [(when selected? "is-active")]}
                              (:title song)]])]])])

(defn ^:private audio-player [_ _]
  (let [audio-ref (r/atom nil)]
    (r/create-class
      {:component-did-update
       (fn [this old-argv]
         (when-let [audio @audio-ref]
           (when-not (= (get-in (second old-argv) [:song :song-url])
                        (get-in (second (r/argv this)) [:song :song-url]))
             (doto audio
               .pause
               (aset "oncanplaythrough" (fn [] (.play audio)))
               .load))))

       :reagent-render
       (fn [{selected-song :song :keys [percentage]} songs]
         [:div.column.spaced.audio-player
          [:div.info.column.spaced
           [album-details (:album selected-song)]
           [:div {:style {:flex-grow 1}}
            [song-list audio-ref selected-song songs]]]
          [wave-form {:audio      @audio-ref
                      :height     100
                      :width      300
                      :percentage percentage}
           (:waveform-1-url selected-song)
           (:waveform-2-url selected-song)]
          [:audio {:cross-origin   "true"
                   :controls       true
                   :controlsList   :nodownload
                   :ref            (partial reset! audio-ref)
                   :on-time-update (fn [x]
                                     (let [target (.-target x)
                                           percentage (/ (.-currentTime target)
                                                         (.-duration target))]
                                       (store/dispatch (actions/update-percentage percentage))))}
           [:source {:src  (:song-url selected-song)
                     :type (:mime-type selected-song)}]]])})))

(defn root* [{:keys [selected-song songs]}]
  [:div
   (if (seq songs)
     [audio-player selected-song songs]
     [:<>
      [:p "We don't have any songs to share with you right now."]
      [:p "Check back soon."]])])

(defn root [_state]
  (store/dispatch actions/fetch-songs)
  (partial components/with-status #{:songs} root*))
