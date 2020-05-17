(ns com.left-over.api.services.db.models.users-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are async deftest is testing]]
    [com.left-over.api.services.db.models.users :as users]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.test.utils :as u]
    [com.left-over.shared.utils.dates :as dates]))

(deftest find-by-email-test
  (testing "(find-by-email)"
    (async done
      (async/go
        (let [conn (u/->DbConn [[{:id "user-id" :name "name"}]])
              [status result] (async/<! (u/prom->ch (users/find-by-email conn "email@foo.com")))
              query (colls/only! @conn)]
          (testing "returns the user"
            (is (= :success status))
            (is (= {:id "user-id" :name "name"} result))
            (is (:select query))
            (is (= [:= :users.email "email@foo.com"]
                   (:where query)))))

        (done)))))

(deftest find-by-id-test
  (testing "(find-by-id)"
    (async done
      (async/go
        (let [conn (u/->DbConn [[{:id "user-id" :name "name"}]])
              [status result] (async/<! (u/prom->ch (users/find-by-id conn "id")))
              query (colls/only! @conn)]
          (testing "returns the user"
            (is (= :success status))
            (is (= {:id "user-id" :name "name"} result))
            (is (:select query))
            (is (= [:= :users.id "id"]
                   (:where query)))))

        (done)))))

(deftest merge-token-info-test
  (testing "(merge-token-info)"
    (async done
      (async/go
        (let [user-id (random-uuid)
              date (dates/now)
              conn (u/->DbConn [[{:id "user-id" :name "name"}]])
              [status result] (async/<! (u/prom->ch (with-redefs [dates/now (constantly date)]
                                                      (users/merge-token-info conn
                                                                              user-id
                                                                              {:expires_in 1234
                                                                               :some       "info"}))))
              query (colls/only! @conn)]
          (testing "returns the user"
            (is (= :success status))
            (is (= {:id "user-id" :name "name"} result))
            (is (:update query))
            (is (= date (get-in query [:set :updated-at])))
            (is (= {:expires_at (dates/stringify (dates/plus date 1234 :seconds))
                    :expires_in 1234
                    :some       "info"}
                   (-> query
                       meta
                       :token-info
                       second
                       (js->clj :keywordize-keys true))))))

        (done)))))
