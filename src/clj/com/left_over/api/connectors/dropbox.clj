(ns com.left-over.api.connectors.dropbox
  (:require [clojure.string :as string]
            [com.ben-allred.vow.core :as v]
            [com.left-over.common.services.env :as env]
            [com.left-over.common.services.http :as http]
            [com.left-over.common.utils.logging :as log]
            [ring.util.mime-type :as mime-type]))

(def ^:private dropbox-list-folder "https://api.dropboxapi.com/2/files/list_folder")

(def ^:private dropbox-temp-link "https://api.dropboxapi.com/2/files/get_temporary_link")

(defn ^:private post [url body]
  (http/post url
             {:headers {:authorization (format "Bearer %s" (env/get :dropbox-access-token))}
              :body    body}))

(defn fetch-photos []
  (-> dropbox-list-folder
      (post {:path                                (env/get :dropbox-images-path)
             :recursive                           false
             :include_media_info                  false
             :include_deleted                     false
             :include_has_explicit_shared_members false
             :include_mounted_folders             false
             :include_non_downloadable_files      false})
      (v/then (fn [{:keys [entries]}]
                (->> entries
                     (map (fn [{:keys [path_lower]}]
                            (post dropbox-temp-link {:path path_lower})))
                     v/all)))
      (v/then-> (->> (filter (comp #(string/starts-with? (str %) "image/")
                                   mime-type/ext-mime-type
                                   :name
                                   :metadata))
                     (map #(-> %
                               (select-keys #{:link :metadata})
                               (update :metadata select-keys #{:name})))))))
