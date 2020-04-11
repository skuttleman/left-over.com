(ns com.left-over.shared.utils.json)

(defn stringify [payload]
  (js/JSON.stringify (clj->js payload)))

(defn parse [s]
  (js->clj (js/JSON.parse s) :keywordize-keys true))
