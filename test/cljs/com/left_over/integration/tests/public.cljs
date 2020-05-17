(ns com.left-over.integration.tests.public
  (:require
    [clojure.test :refer [are is testing]]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.server :as server]
    [com.left-over.api.services.db.models.locations :as locations]
    [com.left-over.api.services.db.models.shows :as shows]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.integration.services.selenium :as sel]
    [com.left-over.integration.services.simulator :as sim]
    [com.left-over.integration.services.webserver :as web]
    [com.left-over.shared.services.http :as http]
    [com.left-over.shared.utils.dates :as dates]))

(defn about-test [{:keys [driver]}]
  (testing "when visiting the about page"
    (v/and (-> driver
               (sel/visit! web/HOME)
               (v/then-> (sel/find (sel/by-link-text "enter")) .click))
           (sel/wait driver (sel/until-url-contains "/about"))
           (v/await [text (-> driver
                              (sel/find (sel/by-css "body"))
                              (v/then sel/get-text))]
             (testing "contains bios for all members"
               (is (re-find #"Scott" text))
               (is (re-find #"Donnie" text))
               (is (re-find #"Brian" text))
               (is (re-find #"Jack" text))
               (is (re-find #"John" text)))))))

(defn shows-test [{:keys [driver user-id]}]
  (testing "when visiting the shows page"
    (v/and (-> driver
               (sel/visit! web/HOME)
               (v/then-> (sel/find (sel/by-link-text "enter")) .click))
           (sel/wait driver (sel/until-url-contains "/about"))
           (-> driver
               (sel/find (sel/by-link-text "shows"))
               (v/then-> .click))
           (sel/wait driver (sel/until-page-contains driver "Upcoming Shows"))
           (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
           (v/await [text (-> driver
                              (sel/find (sel/by-css "body"))
                              (v/then sel/get-text))]
             (testing "does not contain shows"
               (is (re-find #"(?i)we don't have any upcoming shows booked" text))
               (is (re-find #"(?i)check back soon" text))
               (is (re-find #"(?i)no past shows" text))))

           (testing "and when loading shows"
             (v/and (repos/transact (fn [conn]
                                      (v/await [[location-1 location-2]
                                                (v/all [(locations/save conn
                                                                        {:id user-id}
                                                                        {:name  "location 1"
                                                                         :city  "Somewhere"
                                                                         :state "MD"})
                                                        (locations/save conn
                                                                        {:id user-id}
                                                                        {:name  "location 2"
                                                                         :city  "Nowhere"
                                                                         :state "MD"})])]
                                        (v/all [(shows/save conn
                                                            {:id user-id}
                                                            {:name        "Show 1"
                                                             :location-id (:id location-1)
                                                             :date-time   (dates/minus (dates/now) 1 :days)})
                                                (shows/save conn
                                                            {:id user-id}
                                                            {:name        "Show 2"
                                                             :location-id (:id location-2)
                                                             :date-time   (dates/plus (dates/now) 1 :days)})
                                                (shows/save conn
                                                            {:id user-id}
                                                            {:name        "Show 3"
                                                             :location-id (:id location-1)
                                                             :date-time   (dates/plus (dates/now) 2 :days)})]))))
                    (http/with-client http/get (web/API "/public/shows"))
                    (-> driver
                        (sel/find (sel/by-link-text "about"))
                        (v/then-> .click))
                    (sel/wait driver (sel/until-url-contains "/about"))
                    (-> driver
                        (sel/find (sel/by-link-text "shows"))
                        (v/then-> .click))
                    (sel/wait driver (sel/until-page-contains driver "Upcoming Shows"))
                    (v/await [text (-> driver
                                       (sel/find (sel/by-css "body"))
                                       (v/then sel/get-text))]
                      (testing "contains shows"
                        (is (re-find #"Show 1" text))
                        (is (re-find #"Show 2" text))
                        (is (re-find #"Show 3" text))
                        (is (re-find #"location 1" text))
                        (is (re-find #"location 2" text))))))

           (testing "and when the shows are unavailable"
             (v/peek (v/and (server/stop! server/server)
                            (-> driver
                                (sel/find (sel/by-link-text "about"))
                                (v/then-> .click))
                            (sel/wait driver (sel/until-url-contains "/about"))
                            (-> driver
                                (sel/find (sel/by-link-text "shows"))
                                (v/then-> .click))
                            (sel/wait driver (sel/until-url-contains "/shows"))
                            (v/await [text (-> driver
                                               (sel/find (sel/by-css "body"))
                                               (v/then sel/get-text))]
                              (testing "An error is displayed"
                                (is (re-find #"something went wrong" text)))))
                     (fn [_] (server/start! server/server)))))))

(defn images-test [{:keys [driver]}]
  (testing "when visiting the images page"
    (v/and (-> driver
               (sel/visit! web/HOME)
               (v/then-> (sel/find (sel/by-link-text "enter")) .click))
           (sel/wait driver (sel/until-url-contains "/about"))
           (-> driver
               (sel/find (sel/by-link-text "photos"))
               (v/then-> .click))
           (sel/wait driver (sel/until-url-contains "/photos"))
           (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
           (v/await [photos (sel/select driver (sel/by-css ".photo img"))
                     [src-1 src-2 src-3] (v/all (map #(sel/attr % :src) photos))]
             (testing "displays the photos"
               (is (= 3 (count photos)))
               (is (= "http://example.com/img_1.jpg" src-1))
               (is (= "http://example.com/img_2.jpg" src-2))
               (is (= "http://example.com/img_3.jpg" src-3))))

           (testing "and when the images are unavailable"
             (v/peek (v/and (server/stop! sim/server)
                            (-> driver
                                (sel/find (sel/by-link-text "about"))
                                (v/then-> .click))
                            (sel/wait driver (sel/until-url-contains "/about"))
                            (-> driver
                                (sel/find (sel/by-link-text "photos"))
                                (v/then-> .click))
                            (sel/wait driver (sel/until-url-contains "/photos"))
                            (v/await [text (-> driver
                                               (sel/find (sel/by-css "body"))
                                               (v/then sel/get-text))]
                              (testing "An error is displayed"
                                (is (re-find #"something went wrong" text)))))
                     (fn [_] (server/start! sim/server)))))))

(defn videos-test [{:keys [driver]}]
  (testing "when visiting the videos page"
    (v/and (-> driver
               (sel/visit! web/HOME)
               (v/then-> (sel/find (sel/by-link-text "enter")) .click))
           (sel/wait driver (sel/until-url-contains "/about"))
           (-> driver
               (sel/find (sel/by-link-text "videos"))
               (v/then-> .click))
           (sel/wait driver (sel/until-url-contains "/videos"))
           (sel/wait driver (sel/until-not-located driver (sel/by-css ".loader")))
           (v/await [videos (sel/select driver (sel/by-css ".video video source"))
                     [src-1 src-2] (v/all (map #(sel/attr % :src) videos))]
             (testing "displays the video"
               (is (= 2 (count videos)))
               (is (= "http://example.com/video_1.mov" src-1))
               (is (= "http://example.com/video_2.mov" src-2))))

           (testing "and when the videos are unavailable"
             (v/peek (v/and (server/stop! sim/server)
                            (-> driver
                                (sel/find (sel/by-link-text "about"))
                                (v/then-> .click))
                            (sel/wait driver (sel/until-url-contains "/about"))
                            (-> driver
                                (sel/find (sel/by-link-text "videos"))
                                (v/then-> .click))
                            (sel/wait driver (sel/until-url-contains "/videos"))
                            (v/await [text (-> driver
                                               (sel/find (sel/by-css "body"))
                                               (v/then sel/get-text))]
                              (testing "An error is displayed"
                                (is (re-find #"something went wrong" text)))))
                     (fn [_] (server/start! sim/server)))))))

(defn contact-test [{:keys [driver]}]
  (testing "when visiting the contact page"
    (v/and (-> driver
               (sel/visit! web/HOME)
               (v/then-> (sel/find (sel/by-link-text "enter")) .click))
           (sel/wait driver (sel/until-url-contains "/about"))
           (-> driver
               (sel/find (sel/by-link-text "contact"))
               (v/then-> .click))
           (sel/wait driver (sel/until-url-contains "/contact"))
           (v/await [text (-> driver
                              (sel/find (sel/by-css "body"))
                              (v/then sel/get-text))]
             (testing "contains contact info"
               (is (re-find #"(?i)call:" text))
               (is (re-find #"(?i)email:" text))
               (is (re-find #"(?i)facebook:" text)))))))
