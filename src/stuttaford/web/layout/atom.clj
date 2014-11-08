(ns stuttaford.web.layout.atom
  (:use [plumbing.core])
  (:require [clj-time.format :as time-format]
            [clj-time.coerce :as time-coerce]
            [hiccup.core :refer [html]]
            [hiccup.page :as page]
            [hiccup.util :as util]
            [stuttaford.web.posts :as posts]))

(defmacro xml
  [options & contents]
  (if-not (map? options)
    `(xml {} ~options ~@contents)
    `(let [options# ~options]
       (html {:mode :xml}
         (page/xml-declaration (options# :encoding "UTF-8"))
         ~@contents))))

(def atom-timestamp-format
  (comp (partial time-format/unparse (time-format/formatters :date-time-no-ms))
        time-coerce/from-long))

(defnk atom-entry [[:config url] [:post title last-modified permalink content]]
  (let [post-url (str url permalink)]
    [:entry
     [:title title]
     [:link {:href post-url}]
     [:updated (atom-timestamp-format last-modified)]
     [:id post-url]
     [:content {:type "html"} (-> content util/escape-html)]]))

(defnk atom-layout [base-url url [:meta title] [:author name email] :as config]
  (let [all-posts (posts/list-posts)]
    (xml
     [:feed {:xmlns "http://www.w3.org/2005/Atom"}
      [:title title]
      [:link {:href (str url base-url "atom.xml") :rel "self"}]
      [:link {:href (str url base-url)}]
      [:updated (-> all-posts first :last-modified atom-timestamp-format)]
      [:id url]
      [:author
       [:name name]
       [:email email]]
      (map (fn [post] (atom-entry {:config config :post post})) all-posts)])))