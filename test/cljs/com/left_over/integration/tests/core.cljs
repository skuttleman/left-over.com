(ns com.left-over.integration.tests.core
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [async deftest is testing]]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.server :as server]
    [com.left-over.api.services.db.migrations :as migrations]
    [com.left-over.api.services.db.repositories.core :as repos]
    [com.left-over.api.services.env :as env]
    [com.left-over.integration.services.selenium :as sel]
    [com.left-over.integration.services.simulator :as sim]
    [com.left-over.integration.services.webserver :as web]
    [com.left-over.integration.tests.admin :as admin]
    [com.left-over.integration.tests.public :as public]))

(def ^:private count-migrations
  "SELECT count(id)
  FROM ragtime_migrations")

(def ^:private insert-admin
  "INSERT INTO users
  (first_name, last_name, email, external_id, updated_at)
  VALUES
  ('admin','user','admin@user.com','external-123',now())
  RETURNING id")

(defn setup! []
  (v/and (v/all (map server/start! [web/server server/server sim/server]))
         (v/await [migrations (repos/transact repos/exec-raw! [count-migrations])
                   cnt (:count (first migrations))
                   _ (when (pos? cnt)
                       (migrations/rollback! cnt false))
                   _ (migrations/migrate! false)
                   [{user-id :id}] (repos/transact repos/exec-raw! [insert-admin])
                   driver (sel/driver "chrome")]
           {:driver  driver
            :user-id user-id})))

(deftest integration-tests
  (is (= "test" (env/get :environment)) "Failed to load correct env")
  (when (= "test" (env/get :environment))
    (async done
      (async/go
        (-> (setup!)
            (v/then (fn [ctx]
                      (-> (public/about-test ctx)
                          (v/and (public/shows-test ctx)
                                 (public/images-test ctx)
                                 (public/videos-test ctx)
                                 (public/contact-test ctx)
                                 (admin/login-test ctx)
                                 (admin/create-show-test ctx)
                                 (admin/manage-events-test ctx))
                          (v/peek (fn [_]
                                    (sel/quit (:driver ctx)))))))
            (v/catch (fn [err]
                       (is (nil? (or (some-> err .-stack)
                                     err
                                     "An unexpected error occurred")))))
            (v/peek (fn [_]
                      (v/and (v/all (map server/stop! [web/server server/server sim/server]))
                             (done)))))))))
