(ns com.left-over.api.services.dropbox-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are async deftest is testing]]
    [com.left-over.api.services.dropbox :as dropbox]
    [com.left-over.api.services.env :as env]
    [com.left-over.test.utils :as u]))

(def ^:private responses
  {[:post (env/get :dropbox-list-folder)]
   {:entries [{:path_lower "lower_path_1"}
              {:path_lower "lower_path_2"}
              {:path_lower "lower_path_3"}
              {:path_lower "lower_path_4"}
              {:path_lower "lower_path_5"}
              {:path_lower "lower_path_6"}
              {:path_lower "lower_path_7"}]}
   [:post (env/get :dropbox-temp-link)]
   (u/returning {:metadata {:name  "image1.png"
                            :extra "value"
                            :size  0}
                 :link     "link1"}
                {:metadata {:name "file1.extension.voodoo"
                            :size 0}
                 :link     "voodoo"}
                {:metadata {:name  "name.mov"
                            :extra "value"
                            :size  1456}
                 :link     "link2"}
                {:metadata {:name  "name.jpg"
                            :extra "value"
                            :size  14}
                 :link     "link3"}
                {:metadata {:name "file2.mp3"
                            :size 456}
                 :link     "song2"}
                {:metadata {:name "file1.mp3"
                            :size 123}
                 :link     "song1"}
                {:metadata {:name "file1.more"
                            :size 0}
                 :link     "more"})})

(deftest fetch*-test
  (testing "(fetch*)"
    (async done
      (async/go
        (testing "when filtering by image mime-type"
          (let [client (u/->HttpClient responses)
                [status result] (async/<! (u/prom->ch (dropbox/fetch* client
                                                                      "some-path"
                                                                      {:mime-filter dropbox/image?})))
                requests (map #(nth % 2) @client)]
            (is (= :success status))
            (is (= [{:link     "link1"
                     :metadata {:mime-type "image/png"
                                :name      "image1.png"
                                :size      0}}
                    {:link     "link3"
                     :metadata {:mime-type "image/jpeg"
                                :name      "name.jpg"
                                :size      14}}]
                   result))
            (is (= [{:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path                                "some-path"
                               :recursive                           false
                               :include_media_info                  false
                               :include_deleted                     false
                               :include_has_explicit_shared_members false
                               :include_mounted_folders             false
                               :include_non_downloadable_files      false}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_1"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_2"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_3"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_4"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_5"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_6"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_7"}
                     :json?   true}]
                   requests))))

        (testing "when filtering by video mime-type"
          (let [client (u/->HttpClient responses)
                [status result] (async/<! (u/prom->ch (dropbox/fetch* client
                                                                      "some-path"
                                                                      {:mime-filter dropbox/video?})))
                requests (map #(nth % 2) @client)]
            (is (= :success status))
            (is (= [{:link     "link2"
                     :metadata {:name      "name.mov"
                                :mime-type "video/quicktime"
                                :size      1456}}]
                   result))
            (is (= [{:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path                                "some-path"
                               :recursive                           false
                               :include_media_info                  false
                               :include_deleted                     false
                               :include_has_explicit_shared_members false
                               :include_mounted_folders             false
                               :include_non_downloadable_files      false}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_1"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_2"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_3"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_4"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_5"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_6"}
                     :json?   true}
                    {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
                     :body    {:path "lower_path_7"}
                     :json?   true}]
                   requests))))

        (done)))))
