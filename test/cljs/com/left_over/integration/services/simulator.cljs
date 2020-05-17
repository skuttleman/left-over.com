(ns com.left-over.integration.services.simulator
  (:require
    [cljstache.core :as stache]
    [com.ben-allred.espresso.core :as es]
    [com.ben-allred.espresso.middleware :as es.mw]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.server :as server]
    [com.left-over.api.services.fs :as fs]
    [com.left-over.shared.utils.dates :as dates]
    [com.left-over.shared.utils.json :as json]
    [com.left-over.shared.utils.maps :as maps]
    [com.left-over.shared.utils.uri :as uri]))

(defn ^:private ->calendar-item [idx date]
  {:id         (str "event-" idx)
   :summary    (cond-> (str "summary " idx)
                 (even? idx) (str " : " (rand-nth ["rehearsal" "rehearsing" "dnb" "DNB"])))
   :start-date (dates/stringify date)
   :end-date   (dates/stringify (dates/plus date 2 :hours))})

(def ^:private fixtures
  {[:post "/dropbox/list_folder"]
   [200 ["dropbox/folder.json"]]

   [:post "/dropbox/get_temporary_link" {:path "image_1"}]
   [200 ["dropbox/file.json" {:file-name "img_1.jpg"}]]

   [:post "/dropbox/get_temporary_link" {:path "image_2"}]
   [200 ["dropbox/file.json" {:file-name "img_2.jpg"}]]

   [:post "/dropbox/get_temporary_link" {:path "image_3"}]
   [200 ["dropbox/file.json" {:file-name "img_3.jpg"}]]

   [:post "/dropbox/get_temporary_link" {:path "video_1"}]
   [200 ["dropbox/file.json" {:file-name "video_1.mov"}]]

   [:post "/dropbox/get_temporary_link" {:path "video_2"}]
   [200 ["dropbox/file.json" {:file-name "video_2.mov"}]]

   [:get "/oauth/auth"]
   [301 nil {"content-type" "text/html"
             "location"     "http://localhost:3100/auth/callback?state=%7B%3Aredirect-uri%20%22http%3A%2F%2Flocalhost%3A3449%2Fadmin%22%7D&code=987654321"}]

   [:post "/oauth/token" {:code "987654321"}]
   [200 ["google/tokens.json" {:access-token  "access-token"
                               :refresh-token "refresh-token"
                               :expires-in    1000}]]

   [:get "/oauth/tokeninfo" {:access_token "access-token"}]
   [200 ["google/token-info.json" {:email "admin@user.com"}]]

   [:get "/calendar/left.over.band.md@gmail.com/events"]
   [200 ["google/events.json" {:items (->> (dates/now)
                                           (iterate #(dates/plus % 10 :days))
                                           (map-indexed ->calendar-item)
                                           (drop 1)
                                           (take 3))}]]})

(defn ^:private request->fixture [{:keys [method path body query]} fixtures]
  (->> fixtures
       (filter (fn [[[method' path' params]]]
                 (and (= [method' path'] [method path])
                      (or (maps/submap? params body)
                          (maps/submap? params query)))))
       first
       second))

(def server
  (let [handler (fn [request]
                  (let [request (-> request
                                    (cond->
                                      (and (not-empty (:body request))
                                           (= "application/json" (get-in request [:headers "content-type"])))
                                      (update :body json/parse)

                                      (and (not-empty (:body request))
                                           (= "application/x-www-form-urlencoded" (get-in request [:headers "content-type"])))
                                      (update :body uri/split-query))
                                    (assoc :query (uri/split-query (:query-string request))))
                        [status [fixture data] headers] (or (request->fixture request fixtures)
                                                            [404])
                        stub {:status status :headers headers}]

                    (if fixture
                      (-> (str "test/fixtures/" fixture)
                          fs/read-file
                          (v/then (fn [body]
                                    (-> body
                                        str
                                        (cond-> data (stache/render data)))))
                          (v/then (fn [body]
                                    (-> stub
                                        (update-in [:headers "Content-Type"] #(or % "application/json"))
                                        (assoc :body body)))))
                      (v/resolve stub))))]
    (-> handler
        es.mw/with-body
        es/create-server
        (server/->WebServer "Simulator" 8000))))
