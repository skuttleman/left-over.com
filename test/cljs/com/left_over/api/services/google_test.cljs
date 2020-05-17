(ns com.left-over.api.services.google-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are async deftest is testing]]
    [com.ben-allred.vow.core :as v]
    [com.left-over.api.services.env :as env]
    [com.left-over.api.services.google :as google]
    [com.left-over.shared.services.protocols :as p]
    [com.left-over.shared.utils.colls :as colls]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.edn :as edn]
    [com.left-over.shared.utils.strings :as strings]
    [com.left-over.shared.utils.uri :as uri]
    [com.left-over.test.utils :as u]))

(deftest with-oauth-test
  (testing "(with-oauth)"
    (async done
      (async/go
        (let [client (u/->HttpClient {[:post (env/get :oauth-token-uri)]
                                      {:a :token}

                                      [:get (env/get :oauth-token-info-uri)]
                                      {:a :profile}})
              oauth (google/->oauth client)]
          (testing "produces a redirect-uri"
            (let [uri (p/redirect-uri oauth {:my "state"})]
              (is (= {:my "state"}
                     (edn/parse (get-in (uri/parse uri) [:query :state]))))))

          (testing "requests a token"
            (u/init! client)
            (let [[status result] (async/<! (u/prom->ch (p/token oauth {:some "param"})))
                  calls @client]
              (is (= :success status))
              (is (= {:a :token} result))
              (is (= 1 (count calls)))
              (is (= {:some          "param"
                      :client_id     (env/get :oauth-client-id)
                      :client_secret (env/get :oauth-client-secret)}
                     (uri/split-query (:body (nth (first calls) 2)))))))

          (testing "requests a profile"
            (u/init! client)
            (let [access-token (random-uuid)
                  [status result] (async/<! (u/prom->ch (p/profile oauth access-token)))
                  calls @client]
              (is (= :success status))
              (is (= {:a :profile} result))
              (is (= 1 (count calls)))
              (is (= {:access_token access-token} (:query-params (nth (first calls) 2)))))))

        (done)))))

(deftest fetch-calendar-events-test
  (testing "(fetch-calendar-events)"
    (async done
      (async/go
        (testing "when the token is valid"
          (let [[user-id event-1 event-2 event-3] (repeatedly (comp str random-uuid))
                conn (u/->DbConn [[{:token-info {:expires_at   (dates/plus (dates/now) 30 :minutes)
                                                 :access_token "access--token"}}]])
                client (u/->HttpClient {[:get (strings/format (env/get :google-calendar-events) (env/get :google-calendar-id))]
                                        {:items [{:id          event-1
                                                  :start       :start
                                                  :end         :end
                                                  :description "description"
                                                  :summary     "dnb"}
                                                 {:id          event-2
                                                  :start       :start
                                                  :end         :end
                                                  :description "description"
                                                  :summary     "summary"}
                                                 {:id          event-3
                                                  :start       :start
                                                  :end         :end
                                                  :description "description"
                                                  :summary     "rehearsal"}]}})
                [status results] (async/<! (u/prom->ch (google/fetch-calendar-events conn client nil user-id)))
                query (colls/only! @conn)
                [_ _ request] (colls/only! @client)]
            (testing "fetches the events"
              (is (= :success status))
              (is (:select query))
              (is (= [:= :users.id user-id] (:where query)))
              (is (= [{:id          event-1
                       :start       :start
                       :end         :end
                       :description "description"
                       :show?       false
                       :summary     "dnb"}
                      {:id          event-2
                       :start       :start
                       :end         :end
                       :description "description"
                       :show?       true
                       :summary     "summary"}
                      {:id          event-3
                       :start       :start
                       :end         :end
                       :description "description"
                       :show?       false
                       :summary     "rehearsal"}]
                     results))
              (is (= "Bearer access--token" (get-in request [:headers "authorization"]))))))

        (testing "when the token is expired"
          (let [[user-id event-id] (repeatedly (comp str random-uuid))
                conn (u/->DbConn [[{:token-info {:expires_at    (dates/minus (dates/now) 30 :minutes)
                                                 :access_token  "old-access-token"
                                                 :refresh_token "refresh-token"}}]
                                  []])
                client (u/->HttpClient {[:get (strings/format (env/get :google-calendar-events) (env/get :google-calendar-id))]
                                        {:items [{:id event-id}]}})
                oauth (u/->OAuthProvider {:token {:access_token "new-access-token"
                                                  :other        "details"}})
                [status results] (async/<! (u/prom->ch (google/fetch-calendar-events conn client oauth user-id)))
                [select-q update-q] @conn
                profile (colls/only! @oauth)
                [_ _ request] (colls/only! @client)]
            (testing "refreshes the token"
              (is (:select select-q))
              (is (= [:= :users.id user-id] (:where select-q)))
              (is (= "refresh-token" (:refresh_token (second profile))))
              (is (:update update-q)))

            (testing "fetches the events"
              (is (= :success status))
              (is (= [{:id event-id :show? true}] results))
              (is (= {:other "details" :access_token "new-access-token"}
                     (js->clj (second (:token-info (meta update-q))) :keywordize-keys true)))
              (is (= "Bearer new-access-token" (get-in request [:headers "authorization"]))))))

        (testing "when the initial calendar fetch fails"
          (let [[user-id event-id] (repeatedly (comp str random-uuid))
                conn (u/->DbConn [[{:token-info {:expires_at    (dates/plus (dates/now) 30 :minutes)
                                                 :access_token  "old-access-token"
                                                 :refresh_token "refresh-token"}}]
                                  []])
                client (u/->HttpClient {[:get (strings/format (env/get :google-calendar-events) (env/get :google-calendar-id))]
                                        (u/returning (v/reject "none for you")
                                                     {:items [{:id event-id}]})})
                oauth (u/->OAuthProvider {:token {:access_token "new-access-token"
                                                  :other        "details"}})
                [status results] (async/<! (u/prom->ch (google/fetch-calendar-events conn client oauth user-id)))
                [select-q update-q] @conn
                profile (colls/only! @oauth)
                [[_ _ req-1] [_ _ req-2]] @client]

            (testing "refreshes the token"
              (is (:select select-q))
              (is (= [:= :users.id user-id] (:where select-q)))
              (is (= "refresh-token" (:refresh_token (second profile))))
              (is (:update update-q)))

            (testing "fetches the events"
              (is (= :success status))
              (is (= [{:id event-id :show? true}] results))
              (is (= {:other "details" :access_token "new-access-token"}
                     (js->clj (second (:token-info (meta update-q))) :keywordize-keys true)))
              (is (= "Bearer old-access-token" (get-in req-1 [:headers "authorization"])))
              (is (= "Bearer new-access-token" (get-in req-2 [:headers "authorization"])))))

          (testing "and when refreshing the token fails"
            (let [user-id (str (random-uuid))
                  conn (u/->DbConn [[{:token-info {:expires_at    (dates/minus (dates/now) 30 :minutes)
                                                   :access_token  "old-access-token"
                                                   :refresh_token "refresh-token"}}]])
                  oauth (u/->OAuthProvider {:token (v/reject "no token for you")})
                  [status] (async/<! (u/prom->ch (google/fetch-calendar-events conn nil oauth user-id)))]
              (testing "returns an error"
                (is (= :error status)))))

          (testing "and when refreshing the token"
            (testing "and when the subsequent calendar fetch fails"
              (let [[user-id] (repeatedly (comp str random-uuid))
                    conn (u/->DbConn [[{:token-info {:expires_at    (dates/minus (dates/now) 30 :minutes)
                                                     :access_token  "old-access-token"
                                                     :refresh_token "refresh-token"}}]
                                      []])
                    client (u/->HttpClient {[:get (strings/format (env/get :google-calendar-events) (env/get :google-calendar-id))]
                                            (v/reject "none for you")})
                    oauth (u/->OAuthProvider {:token {:access_token "new-access-token"
                                                      :other        "details"}})
                    [status] (async/<! (u/prom->ch (google/fetch-calendar-events conn client oauth user-id)))]
                (testing "returns an error"
                  (is (= :error status)))))))

        (done)))))
