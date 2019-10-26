(ns com.left-over.api.connectors.dropbox
  (:require [com.ben-allred.vow.core :as v]
            [com.left-over.common.services.http :as http]
            [com.left-over.common.services.env :as env]))

(defn ^:private post [url body]
  (http/post url
             {:headers {:authorization (format "Bearer %s" (env/get :dropbox-access-token))}
              :body    body}))

(defn fetch-photos []
  (-> "https://api.dropboxapi.com/2/files/list_folder"
      (post {:path                                (env/get :dropbox-images-path)
             :recursive                           false
             :include_media_info                  false
             :include_deleted                     false
             :include_has_explicit_shared_members false
             :include_mounted_folders             false
             :include_non_downloadable_files      false})
      (v/then :entries)
      (v/then (fn [entries]
                (->> entries
                     (map (fn [{:keys [path_lower]}]
                            (post "https://api.dropboxapi.com/2/files/get_temporary_link"
                                  {:path path_lower})))
                     (v/all))))
      (v/then (partial map #(-> %
                                (select-keys #{:link :metadata})
                                (update :metadata select-keys #{:name}))))))
