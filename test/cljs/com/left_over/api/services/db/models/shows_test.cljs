(ns com.left-over.api.services.db.models.shows-test
  (:require
    [clojure.test :refer [are async deftest is testing]]
    [clojure.core.async :as async]
    [com.left-over.api.services.db.models.shows :as shows]
    [com.left-over.test.utils :as u]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.dates :as dates]))

(deftest select-for-admin-test
  (testing "(select-for-admin)"
    (async done
      (async/go
        (let [conn (u/->DbConn [[{"shows/name"     "show name"
                                  "creator/name"   "creator name"
                                  "locations/name" "location name"}
                                 {"shows/name"     "another show name"
                                  "creator/name"   "another creator name"
                                  "locations/name" "another location name"}]])
              [status result] (async/<! (u/prom->ch (shows/select-for-admin conn)))
              query (colls/only! @conn)]
          (testing "queries the shows"
            (is (= :success status))
            (is (= [{:name      "show name"
                     :creator   {:name "creator name"}
                     :locations {:name "location name"}}
                    {:name      "another show name"
                     :creator   {:name "another creator name"}
                     :locations {:name "another location name"}}]
                   result))
            (is (:select query))
            (is (= [:and #{[:= :shows.deleted false]
                           [:= :shows.confirmed true]}]
                   ((juxt first (comp set rest)) (:where query))))))

        (done)))))

(deftest select-for-website-test
  (testing "(select-for-website)"
    (async done
      (async/go
        (let [conn (u/->DbConn [[{"shows/name"     "show name"
                                  "creator/name"   "creator name"
                                  "locations/name" "location name"}
                                 {"shows/name"     "another show name"
                                  "creator/name"   "another creator name"
                                  "locations/name" "another location name"}]])
              [status result] (async/<! (u/prom->ch (shows/select-for-website conn)))
              query (colls/only! @conn)]
          (testing "queries the shows"
            (is (= :success status))
            (is (= [{:name      "show name"
                     :creator   {:name "creator name"}
                     :locations {:name "location name"}}
                    {:name      "another show name"
                     :creator   {:name "another creator name"}
                     :locations {:name "another location name"}}]
                   result))
            (is (:select query))
            (is (= [:and #{[:= :shows.deleted false]
                           [:= :shows.confirmed true]
                           [:= :shows.hidden false]}]
                   ((juxt first (comp set rest)) (:where query))))
            (is (= 100 (:limit query)))))

        (done)))))

(deftest find-by-id-test
  (testing "(find-by-id)"
    (async done
      (async/go
        (let [show-id (random-uuid)
              conn (u/->DbConn [[{"shows/id"       show-id
                                  "shows/name"     "show name"
                                  "creator/name"   "creator name"
                                  "locations/name" "location name"}]])
              [status result] (async/<! (u/prom->ch (shows/find-by-id conn show-id)))
              query (colls/only! @conn)]
          (testing "queries the shows"
            (is (= :success status))
            (is (= {:id        show-id
                    :name      "show name"
                    :creator   {:name "creator name"}
                    :locations {:name "location name"}}
                   result))
            (is (:select query))
            (is (= [:and #{[:= :shows.id show-id]
                           [:= :shows.deleted false]}]
                   ((juxt first (comp set rest)) (:where query))))))

        (done)))))

(deftest select-new-event-ids-test
  (testing "(select-new-event-ids)"
    (async done
      (async/go
        (let [[event-id-1 event-id-2 event-id-3 event-id-4 event-id-5 :as event-ids] (repeatedly 5 random-uuid)

              conn (u/->DbConn [[{:shows/event-id event-id-1}
                                 {:shows/event-id event-id-4}
                                 {:shows/event-id event-id-2}]])
              [status result] (async/<! (u/prom->ch (shows/select-new-event-ids conn event-ids)))
              query (colls/only! @conn)]
          (testing "filters out event-ids not found in database"
            (is (= :success status))
            (is (= [[:shows.event-id "shows/event-id"]]
                   (:select query)))
            (is (= [:in :shows.event-id event-ids]
                   (:where query)))
            (is (= #{event-id-3 event-id-5} (set result)))))

        (done)))))

(deftest select-unmerged-test
  (testing "(select-unmerged)"
    (async done
      (async/go
        (let [conn (u/->DbConn [[{:shows/data {:some "data"}}]])
              [status result] (async/<! (u/prom->ch (shows/select-unmerged conn)))
              query (colls/only! @conn)]
          (testing "filters out event-ids not found in database"
            (is (= :success status))
            (is (= [{:data {:some "data"}}] result))
            (is (= [:and #{[:= :shows.deleted false]
                           [:= :shows.confirmed false]}]
                   ((juxt first (comp set rest)) (:where query))))))

        (done)))))

(deftest save-test
  (testing "(save)"
    (async done
      (async/go
        (testing "when creating a show"
          (let [user-id (random-uuid)
                conn (u/->DbConn [[{:id "show-id"}]
                                  [{:shows/data {:some "data"}}]])
                [status result] (async/<! (u/prom->ch (with-redefs [dates/now (constantly ::date)]
                                                        (shows/save conn
                                                                    {:id user-id}
                                                                    {:name "foo bar"}))))
                [insert-q select-q] @conn]
            (testing "inserts and selects the show"
              (is (= :success status))
              (is (= {:data {:some "data"}} result))
              (is (:insert-into insert-q))
              (is (= [{:updated_at ::date
                       :timezone   "America/New_York"
                       :name       "foo bar"
                       :confirmed  true
                       :created_by user-id}]
                     (:values insert-q)))
              (is (:select select-q))
              (is (= [:and #{[:= :shows.id "show-id"]
                             [:= :shows.deleted false]}]
                     ((juxt first (comp set rest)) (:where select-q)))))))

        (testing "when updating a show"
          (let [[user-id show-id] (repeatedly random-uuid)
                conn (u/->DbConn [[{:id "show-id"}]
                                  [{:shows/data {:some "data"}}]])
                [status result] (async/<! (u/prom->ch (with-redefs [dates/now (constantly ::date)]
                                                        (shows/save conn
                                                                    {:id user-id}
                                                                    {:id       show-id
                                                                     :deleted? true
                                                                     :name     "foo bar"}))))
                [insert-q select-q] @conn]
            (testing "updates and selects the show"
              (is (= :success status))
              (is (= {:data {:some "data"}} result))
              (is (:update insert-q))
              (is (= {:updated_at ::date
                      :timezone   "America/New_York"
                      :deleted    true
                      :name       "foo bar"}
                     (:set insert-q)))
              (is (:select select-q))
              (is (= [:and #{[:= :shows.id "show-id"]
                             [:= :shows.deleted false]}]
                     ((juxt first (comp set rest)) (:where select-q)))))))

        (done)))))

(deftest delete-test
  (testing "(delete)"
    (async done
      (async/go
        (let [show-ids (repeatedly 3 random-uuid)
              conn (u/->DbConn [[{:id "show-id"}]])
              [status] (async/<! (u/prom->ch (with-redefs [dates/now (constantly ::date)]
                                               (shows/delete conn show-ids))))
              query (colls/only! @conn)]
          (testing "updates the shows"
            (is (= :success status))
            (is (= {:deleted true :updated_at ::date}
                   (:set query)))
            (is (= [:in :shows.id show-ids]
                   (:where query)))))

        (done)))))

(deftest save-temp-data-test
  (testing "(save-temp-data)"
    (async done
      (async/go
        (let [[user-id & show-ids] (repeatedly 4 random-uuid)
              shows (for [show-id show-ids]
                      {:some {:data [:here (str show-id)]}
                       :id   show-id})
              conn (u/->DbConn [[{:id "show-id"}]])
              [status] (async/<! (u/prom->ch (with-redefs [dates/now (constantly ::date)]
                                               (shows/save-temp-data conn
                                                                     {:id user-id}
                                                                     shows))))
              query (colls/only! @conn)]
          (testing "updates the shows"
            (is (= :success status))
            (is (= (map (fn [event-id]
                          {:updated_at ::date
                           :event_id   event-id
                           :temp_data  {:some {:data ["here" (str event-id)]}}
                           :created_by user-id
                           :hidden     true})
                        show-ids)
                   (map #(update % :temp_data js->clj :keywordize-keys true)
                        (:values query))))))

        (done)))))

(deftest refresh-events-test
  (testing "(refresh-events)"
    (async done
      (async/go
        (let [[user-id & show-ids] (repeatedly 4 random-uuid)
              shows (for [show-id show-ids]
                      {:some {:data [:here (str show-id)]}
                       :id   show-id})
              conn (u/->DbConn [[{:event-id (first show-ids)}]
                                [{:new "results"}]])
              [status result] (async/<! (u/prom->ch (shows/refresh-events conn {:id user-id} shows)))
              [_ insert-q] @conn]
          (testing "updates the shows"
            (is (= :success status))
            (is (:insert-into insert-q))
            (is (= (rest show-ids) (map :event_id (:values insert-q))))
            (is (= [[{:new "results"}]
                    (take 1 shows)]
                   result))))

        (done)))))
