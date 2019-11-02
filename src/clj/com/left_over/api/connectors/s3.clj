(ns com.left-over.api.connectors.s3
  (:require
    [clojure.set :as set]
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as aws.creds]
    [com.left-over.common.services.env :as env]))

(def s3-client (aws/client {:api                  :s3
                            :region               (env/get :s3-api-region)
                            :credentials-provider (aws.creds/basic-credentials-provider
                                                    {:access-key-id     (env/get :s3-api-key)
                                                     :secret-access-key (env/get :s3-api-secret)})}))

(defn fetch [key]
  (try (-> s3-client
           (aws/invoke {:op      :GetObject
                        :request {:Bucket (env/get :s3-api-bucket)
                                  :Key    key}})
           (set/rename-keys {:Body          :body
                             :ContentType   :content-type
                             :ContentLength :content-length}))
       (catch Throwable _
         nil)))
