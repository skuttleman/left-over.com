(ns com.left-over.integration.services.selenium
  (:refer-clojure :exclude [find get])
  (:require
    [com.ben-allred.vow.core :as v]
    [com.left-over.shared.utils.json :as json]
    [com.left-over.shared.utils.uri :as uri]
    selenium-webdriver))

(defn driver [browser]
  (-> (selenium-webdriver/Builder.)
      (.forBrowser browser)
      .build
      v/native->prom))

(defn visit! [driver url]
  (-> driver
      (.get url)
      v/native->prom
      (v/then (constantly driver))))

(defn send-keys [element text]
  (-> element
      (.sendKeys text)
      v/native->prom
      (v/then (constantly element))))

(defn by-id [id]
  (selenium-webdriver/By.id id))

(defn by-css [css]
  (selenium-webdriver/By.css css))

(defn by-link-text [text]
  (selenium-webdriver/By.linkText text))

(defn by-name [name]
  (selenium-webdriver/By.name name))

(defn find [driver selector]
  (-> driver
      (.findElement selector)
      v/native->prom))

(defn select [driver selector]
  (-> driver
      (.findElements selector)
      v/native->prom))

(defn attr [element attr]
  (-> element
      (.getAttribute (name attr))
      v/native->prom))

(defn quit [driver]
  (-> driver
      .quit
      v/native->prom))

(defn get-text [element]
  (-> element
      .getText
      v/native->prom))

(defn url [driver]
  (-> driver
      .getCurrentUrl
      v/native->prom))

(defn click [element]
  (-> element
      .click
      v/native->prom))

(defn wait
  ([driver condition]
   (wait driver condition 2000))
  ([driver condition ms]
   (-> condition
       (cond->
         (not (v/promise? condition)) v/resolve)
       (v/then (fn [condition]
                 (when condition
                   (-> driver
                       (.wait condition ms)
                       v/native->prom))))
       (v/then (constantly driver)))))

(defn until-path-is [path]
  (selenium-webdriver/Condition. (str "for path to be " (json/stringify path))
                                 (fn [driver]
                                   (-> driver
                                       .getCurrentUrl
                                       v/native->prom
                                       (v/then-> uri/parse :path #{path})))))

(defn until-url-contains [url-fragment]
  (selenium-webdriver/until.urlContains url-fragment))

(defn until-url-matches [re]
  (selenium-webdriver/until.urlMatches re))

(defn until-element-contains [element text]
  (selenium-webdriver/until.elementTextContains element text))

(defn until-page-contains [driver text]
  (v/await [body (find driver (by-css "body"))]
    (until-element-contains body text)))

(defn until-not-located [driver selector]
  (v/and (v/sleep 50)
         (v/then (find driver selector)
                 selenium-webdriver/until.stalenessOf
                 (constantly nil))))

(defn until-located [selector]
  (selenium-webdriver/until.elementLocated selector))
