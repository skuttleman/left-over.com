(ns com.left-over.api.services.dropbox
  (:require
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.left-over.api.services.env :as env]
    [com.left-over.shared.services.http :as http]
    [com.left-over.shared.utils.memoize :as memo]
    [com.left-over.shared.utils.numbers :as numbers]
    [com.left-over.shared.utils.logging :as log]))

(def ^:private mime-types
  {"avi"  "video/x-msvideo"
   "bmp"  "image/bmp"
   "gif"  "image/gif"
   "ico"  "image/x-icon"
   "jpe"  "image/jpeg"
   "jpeg" "image/jpeg"
   "jpg"  "image/jpeg"
   "mov"  "video/quicktime"
   "m4v"  "video/mp4"
   "mp3"  "audio/mpeg"
   "mp4"  "video/mp4"
   "mpe"  "video/mpeg"
   "mpeg" "video/mpeg"
   "mpg"  "video/mpeg"
   "oga"  "audio/ogg"
   "ogg"  "audio/ogg"
   "ogv"  "video/ogg"
   "png"  "image/png"
   "qt"   "video/quicktime"
   "tif"  "image/tiff"
   "tiff" "image/tiff"
   "wmv"  "video/x-ms-wmv"})

(defn ^:private with-mime-type [{:keys [name] :as metadata}]
  (assoc metadata :mime-type (some-> name
                                     string/lower-case
                                     (string/split #"\.")
                                     peek
                                     mime-types)))

(defn image? [mime-type]
  (some-> mime-type (string/starts-with? "image/")))

(defn video? [mime-type]
  (some-> mime-type (string/starts-with? "video/")))

(defn ^:private request [client url body]
  (http/post client
             url
             {:headers {:authorization (str "Bearer " (env/get :dropbox-access-token))}
              :body    body
              :json?   true}))

(defn fetch* [client mime-filter path]
  (v/await [{:keys [entries]} (request client
                                       (env/get :dropbox-list-folder)
                                       {:path                                path
                                        :recursive                           false
                                        :include_media_info                  false
                                        :include_deleted                     false
                                        :include_has_explicit_shared_members false
                                        :include_mounted_folders             false
                                        :include_non_downloadable_files      false})
            results (->> entries
                         (map (fn [{:keys [path_lower]}]
                                (v/or (request client
                                               (env/get :dropbox-temp-link)
                                               {:path path_lower})
                                      nil)))
                         v/all)]
    (->> results
         (map #(update % :metadata with-mime-type))
         (filter (comp mime-filter :mime-type :metadata))
         (map #(-> %
                   (select-keys #{:link :metadata})
                   (update :metadata select-keys #{:mime-type :name :size}))))))

(def ^{:arglists '([event])} fetch-images
  (memo/memo (fn [_event] (http/with-client fetch* image? (env/get :dropbox-images-path)))
             (js/parseInt (env/get :dropbox-cache-ttl))
             (js/parseInt (env/get :dropbox-max-cache-ttl))))

(def ^{:arglists '([event])} fetch-videos
  (memo/memo (fn [_event] (http/with-client fetch* video? (env/get :dropbox-videos-path)))
             (numbers/parse-int (env/get :dropbox-cache-ttl))
             (numbers/parse-int (env/get :dropbox-max-cache-ttl))))
