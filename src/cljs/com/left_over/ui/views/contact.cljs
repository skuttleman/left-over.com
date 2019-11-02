(ns com.left-over.ui.views.contact)

(defn root [_]
  [:div.contact-info
   [:p "Call: " [:a {:href "tel:+14108687266"} "(410) 868-7266"]]
   [:p "Email: " [:a {:href "mailto:john@innersoundstudio.com"} "john@innersoundstudio.com"]]
   [:p "Facebook: " [:a {:target :_blank :href "http://www.facebook.com/LObandmd"} "http://www.facebook.com/LObandmd"]]])
