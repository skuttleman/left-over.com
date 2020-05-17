(ns com.left-over.ui.admin.services.store.reducers-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.left-over.ui.admin.services.store.reducers :as reducers]))

(deftest forms-test
  (testing "(forms)"
    (testing "initializes form"
      (is (= {:some        :state
              :external-id ::form
              :forms/state {:internal-id ::form-state}}
             (reducers/forms {:some :state}
                             [:forms/init :internal-id :external-id ::form-state ::form]))))

    (testing "updates form state"
      (is (= {:some        :state
              :forms/state {:internal-id {:some :internal-state
                                          :foo  :bar}}}
             (reducers/forms {:some        :state
                              :forms/state {:internal-id {:some :internal-state}}}
                             [:forms/swap! :internal-id assoc :foo :bar]))))))
