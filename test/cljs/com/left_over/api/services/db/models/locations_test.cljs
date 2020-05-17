(ns com.left-over.api.services.db.models.locations-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are async deftest is testing]]
    [com.left-over.api.services.db.models.locations :as locations]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.test.utils :as u]
    [com.ben-allred.vow.core :as v]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.dates :as dates]))

(deftest select-for-admin-test
  (testing "(select-for-admin)"
    (async done
      (async/go
        (let [conn (u/->DbConn [[{:locations/foo "bar"}
                                 {:locations/foo "baz"}]])
              [status result] (async/<! (u/prom->ch (locations/select-for-admin conn)))
              query (colls/only! @conn)]
          (testing "queries the locations"
            (is (= :success status))
            (is (= [{:foo "bar"} {:foo "baz"}] result))
            (is (not (contains? query :where)))))

        (done)))))

(deftest find-by-id-test
  (testing "(find-by-id)"
    (async done
      (async/go
        (let [location-id (random-uuid)
              conn (u/->DbConn [[{:locations/foo "bar"}]])
              [status result] (async/<! (u/prom->ch (locations/find-by-id conn location-id)))
              query (colls/only! @conn)]
          (testing "returns the result"
            (is (= :success status))
            (is (= {:foo "bar"} result))
            (is (= [:= :locations.id location-id] (:where query)))))

        (done)))))

(deftest save-test
  (testing "(save)"
    (async done
      (async/go
        (testing "when saving a new location"
          (with-redefs [dates/now (constantly ::date)]
            (let [new-id (random-uuid)
                  conn (u/->DbConn [[{:id new-id}]
                                    [{:locations/name "a name"}]])
                  [status result] (async/<! (-> conn
                                                (locations/save {:id "user-id"} {:name "saved name"})
                                                u/prom->ch))
                  [insert-q select-q :as queries] @conn]
              (testing "saves and queries the location"
                (is (= 2 (count queries)))
                (is (= :success status))
                (is (= {:name "a name"} result))
                (is (:insert-into insert-q))
                (is (= [{:updated_at ::date :name "saved name" :created_by "user-id"}] (:values insert-q)))
                (is (:select select-q))
                (is (= [:= :locations.id new-id] (:where select-q)))))))

        (testing "when updating an existing location"
          (with-redefs [dates/now (constantly ::date)]
            (let [existing-id (random-uuid)
                  conn (u/->DbConn [[{:id existing-id}]
                                    [{:locations/name "a name"}]])
                  [status result] (async/<! (-> conn
                                                (locations/save {:id "user-id"} {:id existing-id :name "saved name"})
                                                u/prom->ch))
                  [update-q select-q :as queries] @conn]
              (testing "saves and queries the location"
                (is (= 2 (count queries)))
                (is (= :success status))
                (is (= {:name "a name"} result))
                (is (:update update-q))
                (is (= {:updated_at ::date :name "saved name"} (:set update-q)))
                (is (= [:= :locations.id existing-id] (:where update-q)))
                (is (:select select-q))
                (is (= [:= :locations.id existing-id] (:where select-q)))))))

        (done)))))
