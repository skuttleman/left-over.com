(ns com.left-over.api.handlers.auth-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are async deftest is testing]]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.handlers.auth :as auth]
    [com.left-over.api.services.jwt :as jwt]
    [com.left-over.test.utils :as u]))

(def ^:private params
  {:state (pr-str {:redirect-uri "example.com"})})

(deftest handle-callback-test
  (testing "(handle-callback)"
    (async done
      (async/go
        (testing "when authentication succeeds"
          (let [user-id (random-uuid)
                oauth (u/->OAuthProvider {:token   {:access_token "foobar" :details {:more :details}}
                                          :profile {:email "foo@bar.net" :id "123"}})
                conn (u/->DbConn [[{:id user-id}]
                                  [{:id user-id}]])
                [status value] (async/<! (-> conn
                                             (auth/handle-callback oauth params)
                                             u/prom->ch))
                [_ token :as match] (re-matches #"example\.com\?token=(.+)" value)
                [select-q update-q :as qs] @conn]
            (testing "returns the redirect-uri"
              (is (= :success status))
              (is match)
              (is (= {:data {:id user-id} :sub (str user-id)}
                     (select-keys (jwt/decode token) #{:data :sub}))))

            (testing "updates the user"
              (is (= 2 (count qs)))
              (is (:select select-q))
              (is (= [:= :users.email "foo@bar.net"]
                     (:where select-q)))
              (is (:update update-q))
              (is (= [:= :users.id user-id]
                     (:where update-q)))
              (is (= {:access_token "foobar"
                      :details      {:more "details"}}
                     (-> update-q
                         meta
                         :token-info
                         second
                         (js->clj :keywordize-keys true)))))))

        (testing "when there is no redirect-uri"
          (let [[status] (async/<! (u/prom->ch (auth/handle-callback nil nil nil)))]
            (testing "resolves with no value"
              (is (= :error status)))))

        (testing "when fetching the access token fails"
          (let [oauth (u/->OAuthProvider {:token {:access_token "foobar" :details {:more :details}}})]
            (let [[status value] (async/<! (u/prom->ch (auth/handle-callback nil oauth params)))]
              (testing "returns the redirect-uri"
                (is (= :success status))
                (is (= "example.com?token-msg-id=auth/failed" value))))))

        (testing "when fetching the token info fails"
          (let [oauth (u/->OAuthProvider {})]
            (let [[status value] (async/<! (u/prom->ch (auth/handle-callback nil oauth params)))]
              (testing "returns the redirect-uri"
                (is (= :success status))
                (is (= "example.com?token-msg-id=auth/failed" value))))))

        (testing "when user lookup fails"
          (let [oauth (u/->OAuthProvider {:token   {:access_token "foobar" :details {:more :details}}
                                          :profile {:email "foo@bar.net" :id "123"}})
                conn (u/->DbConn [(v/reject)])
                [status value] (async/<! (-> conn
                                             (auth/handle-callback oauth params)
                                             u/prom->ch))]
            (is (= :success status))
            (is (= "example.com?token-msg-id=auth/failed" value))))

        (testing "when there is no user in the database"
          (let [oauth (u/->OAuthProvider {:token   {:access_token "foobar" :details {:more :details}}
                                          :profile {:email "foo@bar.net" :id "123"}})
                conn (u/->DbConn [[]])
                [status value] (async/<! (-> conn
                                             (auth/handle-callback oauth params)
                                             u/prom->ch))]
            (is (= :success status))
            (is (= "example.com?token-msg-id=auth/failed" value))))

        (testing "when updating user info fails"
          (let [oauth (u/->OAuthProvider {:token   {:access_token "foobar" :details {:more :details}}
                                          :profile {:email "foo@bar.net" :id "123"}})
                conn (u/->DbConn [[{:id (random-uuid)}]
                                  (v/reject)])
                [status value] (async/<! (-> conn
                                             (auth/handle-callback oauth params)
                                             u/prom->ch))]
            (is (= :success status))
            (is (= "example.com?token-msg-id=auth/failed" value))))

        (done)))))
