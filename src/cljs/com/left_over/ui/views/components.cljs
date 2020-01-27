(ns com.left-over.ui.views.components)

(defn with-status [keys component state]
  (condp #(contains? %2 %1) (into #{} (map (comp first state)) keys)
    :error [:div
            [:p "something went wrong"]
            [:p "please try again later"]]
    :init [:div.loader-container [:div.loader.large]]
    [component (into state (map (juxt identity (comp second state))) keys)]))
