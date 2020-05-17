(ns com.left-over.api.services.db.models.core-test
  (:require [clojure.test :refer [are deftest is testing]]
            [com.left-over.api.services.db.models.core :as models]))

(deftest under-test
  (testing "(under)"
    (testing "nests namespaced keywords that are not the specified root-key"
      (are [root-key items expected] (= expected (into [] (models/under root-key) items))
        :foo []
        []

        :bar [{}]
        [{}]

        :baz [{} {:foo 1 :bar 2}]
        [{} {:foo 1 :bar 2}]

        :quux [{:foo/k 1 :bar/k 2 :baz/k 3 :quux/k 4 :other 5}]
        [{:quux/k 4 :other 5 "foo" {:foo/k 1} "bar" {:bar/k 2} "baz" {:baz/k 3}}]))))
