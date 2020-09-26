(ns com.left-over.integration.tests.admin
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are is]]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.integration.services.selenium :as sel]
    [com.left-over.integration.services.webserver :as web]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.test.macros :refer-macros [testing with-retry]]))

(defn ^:private fill-out! [driver key value]
  (-> driver
      (sel/find (sel/by-name (name key)))
      (v/then (fn continue [element]
                (v/then (sel/attr element :value)
                        (fn [value]
                          (if (seq value)
                            (v/and (sel/send-keys element \backspace)
                                   (continue element))
                            element)))))
      (v/then-> (sel/send-keys value) (v/sleep 50))))

(defn ^:private submit! [driver]
  (-> driver
      (sel/find (sel/by-css "button[type=submit]"))
      (v/then-> sel/click)))

(defn login-test [{:keys [driver user-id]}]
  (testing "when logging in"
    (v/and
      (-> driver
          (sel/visit! web/ADMIN)
          (v/then-> (sel/wait (sel/until-path-is "/admin/login"))
                    (sel/wait (sel/until-page-contains driver "Login with Google") 2000)
                    (sel/find (sel/by-css "a.button.is-link"))
                    sel/click))
      (sel/wait driver (sel/until-path-is "/admin"))
      (sel/wait driver (sel/until-page-contains driver "Logout"))
      (testing "saves token info"
        (v/await [{:keys [token-info]} (repos/transact users/find-by-id user-id)]
          (is (= 1000 (:expires_in token-info)))
          (is (= "access-token" (:access_token token-info)))
          (is (= "refresh-token" (:refresh_token token-info)))
          (is (<= (dates/inst->ms (:expires_at token-info))
                  (dates/inst->ms (dates/plus (dates/now) 1000 :seconds)))))))))

(defn create-show-test [{:keys [driver]}]
  (testing "when logged in"
    (v/and
      (sel/visit! driver web/ADMIN)
      (sel/wait driver (sel/until-page-contains driver "Logout"))
      (-> driver
          (sel/find (sel/by-link-text "Create a show"))
          (v/then-> sel/click))
      (sel/wait driver (sel/until-located (sel/by-css "button[type=submit]")))
      (testing "and when creating a show without data"
        (v/and
          (submit! driver)
          (sel/wait driver (sel/until-located (sel/by-css ".error-list")))
          (testing "displays errors"
            (v/await [text (-> driver
                               (sel/find (sel/by-css "body"))
                               (v/then-> sel/get-text))]
              (is (string/includes? text "You must have a name for the show"))
              (is (string/includes? text "You must select a date/time for the show"))
              (is (string/includes? text "You must select a location"))))

          (testing "disables the submit button"
            (v/await [disabled? (-> driver
                                    (sel/find (sel/by-css "button[type=submit]"))
                                    (v/then-> (sel/attr :disabled)))]
              (is disabled?)))

          (testing "and when filling in the name"
            (v/and
              (fill-out! driver :name "The show name")
              (testing "displays errors"
                (v/await [text (-> driver
                                   (sel/find (sel/by-css "body"))
                                   (v/then-> sel/get-text))]
                  (is (not (string/includes? text "You must have a name for the show")))
                  (is (string/includes? text "You must select a date/time for the show"))
                  (is (string/includes? text "You must select a location"))))

              (testing "disables the submit button"
                (v/await [disabled? (-> driver
                                        (sel/find (sel/by-css "button[type=submit]"))
                                        (v/then-> (sel/attr :disabled)))]
                  (is disabled?)))

              (testing "and when filling in the date-time"
                (v/and
                  (fill-out! driver :date-time "01272200\t0630P")
                  (testing "displays errors"
                    (v/await [text (-> driver
                                       (sel/find (sel/by-css "body"))
                                       (v/then-> sel/get-text))]
                      (is (not (string/includes? text "You must have a name for the show")))
                      (is (not (string/includes? text "You must select a date/time for the show")))
                      (is (string/includes? text "You must select a location"))))

                  (testing "disables the submit button"
                    (v/await [disabled? (-> driver
                                            (sel/find (sel/by-css "button[type=submit]"))
                                            (v/then-> (sel/attr :disabled)))]
                      (is disabled?)))

                  (testing "and when creating a location"
                    (v/and
                      (-> driver
                          (sel/find (sel/by-css ".button.dropdown-create"))
                          (v/then-> sel/click))
                      (sel/wait driver (sel/until-page-contains driver "Create Location"))
                      (v/await [modal (sel/find driver (sel/by-css ".modal-content"))]
                        (testing "and when creating a show without data"
                          (v/and
                            (submit! modal)
                            (sel/wait driver (sel/until-located (sel/by-css ".modal-content .error-list")))
                            (testing "displays errors"
                              (v/await [text (sel/get-text modal)]
                                (is (string/includes? text "You must have a name for the location"))
                                (is (string/includes? text "You must have a city for the location"))))

                            (testing "disables the submit button"
                              (v/await [disabled? (-> modal
                                                      (sel/find (sel/by-css "button[type=submit]"))
                                                      (v/then-> (sel/attr :disabled)))]
                                (is disabled?)))

                            (testing "and when filling in the name"
                              (v/and
                                (fill-out! modal :name "The location name")
                                (testing "displays errors"
                                  (v/await [text (sel/get-text modal)]
                                    (is (not (string/includes? text "You must have a name for the location")))
                                    (is (string/includes? text "You must have a city for the location"))))

                                (testing "disables the submit button"
                                  (v/await [disabled? (-> modal
                                                          (sel/find (sel/by-css "button[type=submit]"))
                                                          (v/then-> (sel/attr :disabled)))]
                                    (is disabled?)))

                                (testing "and when filling in the city"
                                  (v/and
                                    (fill-out! modal :city "Nowheresville")
                                    (submit! modal)
                                    (sel/wait driver (sel/until-not-located driver (sel/by-name "city"))))))))))

                      (testing "and when saving the form"
                        (v/and
                          (submit! driver)
                          (sel/wait driver (sel/until-page-contains driver "Create a show"))
                          (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
                          (testing "displays the newly created show"
                            (v/await [text (-> driver
                                               (sel/find (sel/by-css "body"))
                                               (v/then-> sel/get-text))]
                              (is (string/includes? text "The location name"))
                              (is (string/includes? text "Nowheresville, MD"))
                              (is (string/includes? text "Mon, Jan 27, 2200 @ 6:30 PM")))))))))))))))))

(defn manage-events-test [{:keys [driver]}]
  (testing "when logged in"
    (v/and
      (sel/visit! driver web/ADMIN)
      (sel/wait driver (sel/until-page-contains driver "Logout"))
      (testing "and when fetching events"
        (v/and
          (-> driver
              (sel/find (sel/by-link-text "Manage calendar events"))
              (v/then-> sel/click))
          (sel/wait driver (sel/until-path-is "/admin/calendar"))
          (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
          (testing "contains calendar events"
            (v/await [show-cards (sel/select driver (sel/by-css ".show-events .card"))
                      non-cards (sel/select driver (sel/by-css ".not-show-events .card"))
                      [show-card-1-text show-card-2-text] (v/all (map sel/get-text show-cards))
                      [non-card-1-text non-card-2-text] (v/all (map sel/get-text non-cards))]
              (is (= 2 (count show-cards)))
              (is (= 2 (count non-cards)))
              (is (string/includes? show-card-1-text "summary 1"))
              (is (string/includes? show-card-2-text "summary 3"))
              (is (string/includes? non-card-1-text "rehearsal"))
              (is (string/includes? non-card-2-text "summary 2"))))
          (sel/wait driver (sel/until-located (sel/by-css ".show-events .card")))

          (testing "and when converting a show"
            (v/and
              (v/await [[show-card] (sel/select driver (sel/by-css ".show-events .card"))]
                (-> show-card
                    (sel/find (sel/by-css ".link"))
                    (v/then-> sel/click))
                (sel/wait driver (sel/until-url-contains "/admin/shows/"))
                (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
                (-> driver
                    (sel/find (sel/by-css ".dropdown-trigger .button"))
                    (v/then-> sel/click))
                (sel/wait driver (sel/until-located (sel/by-css ".dropdown-item")))
                (-> driver
                    (sel/find (sel/by-css ".dropdown-item"))
                    (v/then-> sel/click))
                (submit! driver))
              (sel/wait driver (sel/until-path-is "/admin/calendar"))
              (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
              (sel/wait driver (sel/until-located (sel/by-css ".show-events .card")))

              (testing "and when removing a show"
                (v/and
                  (with-retry 3
                    (v/await [[show-card] (sel/select driver (sel/by-css ".show-events .card"))]
                      (-> show-card
                          (sel/find (sel/by-css ".button.is-danger"))
                          (v/then-> sel/click)))
                    (sel/wait driver (sel/until-page-contains driver "Remove Event")))
                  (with-retry 3
                    (v/await [modal (sel/find driver (sel/by-css ".modal-content"))]
                      (-> modal
                          (sel/find (sel/by-css ".button.is-danger"))
                          (v/then-> sel/click)))
                    (sel/wait driver (sel/until-not-located driver (sel/by-css ".modal-content")))
                    (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
                    (sel/wait driver (sel/until-located (sel/by-css ".not-show-events .card"))))

                  (testing "and when converting a non-show"
                    (v/and
                      (v/await [[non-card] (sel/select driver (sel/by-css ".not-show-events .card"))]
                        (-> non-card
                            (sel/find (sel/by-css ".link"))
                            (v/then-> sel/click))
                        (sel/wait driver (sel/until-url-contains "/admin/shows/"))
                        (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
                        (fill-out! driver :date-time "01272200\t0630P")
                        (-> driver
                            (sel/find (sel/by-css ".dropdown-trigger .button"))
                            (v/then-> sel/click))
                        (sel/wait driver (sel/until-located (sel/by-css ".dropdown-item")))
                        (-> driver
                            (sel/find (sel/by-css ".dropdown-item"))
                            (v/then-> sel/click))
                        (submit! driver))
                      (sel/wait driver (sel/until-path-is "/admin/calendar"))
                      (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
                      (sel/wait driver (sel/until-located (sel/by-css ".not-show-events .card")))

                      (testing "and when removing a non-show"
                        (v/and

                          (with-retry 3
                            (v/await [[show-card] (sel/select driver (sel/by-css ".not-show-events .card"))]
                              (-> show-card
                                  (sel/find (sel/by-css ".button.is-danger"))
                                  (v/then-> sel/click)))
                            (sel/wait driver (sel/until-page-contains driver "Remove Event")))
                          (with-retry 3
                            (v/await [modal (sel/find driver (sel/by-css ".modal-content"))]
                              (-> modal
                                  (sel/find (sel/by-css ".button.is-danger"))
                                  (v/then-> sel/click)))
                            (sel/wait driver (sel/until-not-located driver (sel/by-css ".modal-content")))
                            (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader"))))
                          (testing "contains no more events"
                            (v/await [text (-> driver
                                               (sel/find (sel/by-css "body"))
                                               (v/then sel/get-text))]
                              (is (string/includes? text "You have no unmerged shows."))))

                          (testing "and when managing shows"
                            (v/and
                              (-> driver
                                  (sel/find (sel/by-link-text "Manage shows"))
                                  (v/then-> sel/click))
                              (sel/wait driver (sel/until-path-is "/admin"))
                              (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
                              (v/await [text (-> driver
                                                 (sel/find (sel/by-css "body"))
                                                 (v/then sel/get-text))]
                                (testing "contains the converted events"
                                  (is (string/includes? text "summary 1"))
                                  (is (string/includes? text "rehearsal")))

                                (testing "does not contain the removed events"
                                  (is (not (string/includes? text "summary 2")))
                                  (is (not (string/includes? text "summary 3"))))))))))))))))))))
