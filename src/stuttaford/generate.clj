(ns stuttaford.generate
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [peridot.core :as peridot]
            [stuttaford.web.posts :as posts]
            [stuttaford.web.routes :as routes]))

(def SITE "site")

(defn clean []
  (log/info " * Cleaning previous build")
  (fs/delete-dir SITE))

(defn ensure-site []
  (fs/mkdir SITE))

(defn clean-non-prod-js []
  (log/info " * Cleaning non-production js")
  (fs/delete-dir "resources/public/js/out")
  (fs/delete "resources/public/js/stuttaford.js")
  (fs/delete "resources/public/js/manifest.json"))

(defn copy-public-to-site []
  (log/info " * Copying public files")
  (shell/with-sh-dir "."
    (shell/sh "bash" "-c" (format "cp -r resources/public/* %s/." SITE))))

(defn content-for-url [session url]
  (-> session
      (peridot/request url)
      :response
      :body))

(defn ensure-dirs [path]
  (if (re-find #"/" path)
    (-> path
        (string/replace #"/[^/]*$" "")
        fs/mkdirs)))

(defn write-site-file [path content]
  (let [path (str SITE path)]
    (ensure-dirs path)
    (spit path content)))

(defn generate-path [session path filename]
  (when-not (string/starts-with? path "http")
    (log/info " * Generating" path "->" filename)
    (->> path
         (content-for-url session)
         (write-site-file filename))))

(defn generate-html-path [session path]
  (generate-path session path (str path "index.html")))

(defn build []
  (alter-var-root #'routes/PROD-MODE? (constantly true))
  (log/info "=======================================")
  (log/info "Building site")
  (clean)
  (ensure-site)
  (clean-non-prod-js)
  (copy-public-to-site)
  (let [session (peridot/session routes/app)]
    (generate-path session "/" "/index.html")
    (generate-path session "/atom.xml" "/atom.xml")
    (generate-html-path session "/blog/")
    (generate-html-path session "/codex/")
    (doseq [page (:markdown-pages (routes/site-config))]
      (generate-html-path session (str "/" page "/")))
    (generate-path session "/blog/archived/"
                   "/blog/archived/index.html")
    (doseq [permalink (map :permalink (posts/list-posts))]
      (generate-html-path session permalink)))
  (alter-var-root #'routes/PROD-MODE? (constantly false))
  (log/info "Done."))

#_ (build)
