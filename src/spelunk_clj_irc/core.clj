;   Copyright (c) Seth Schroeder. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;   Source of IRC logs: http://clojure-log.n01se.net/

(ns spelunk-clj-irc.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojure.contrib.except :as except]
            [clojure.contrib.sql :as sql]
            [net.cgrand.enlive-html :as html]
            [clj-time.format :as date]
            [clj-time.core :as time]
            [spelunk-clj-irc.util :as util]
           )
  (:import [java.net URL]
           [java.io File])
  (:use    [spelunk-clj-irc.types :as types]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Comment [#^String who, #^String what, #^org.joda.time.DateTime when])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def *start-url* "http://clojure-log.n01se.net/date/2008-02-01.html")
(def *last-person* (atom ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- set-speaker [hash speaker]
  (reset! *last-person* speaker)
  (assoc hash :who speaker))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn preprocess-node [year month day item]
  {:post [(:who %), (:when %), #_(:what %)]} ;; relaxing what constraint, empty messages filtered later
  (reduce (fn [memo node]
            ;; (log/debug "==== Begin node processing")
            ;; ;; hmm.. how to mix clojure.contrib.strint, clojure.pprint, and clojure.contrib.logging?
            ;; (log/trace memo)
            ;; (log/trace node)

            (cond
             (string? node)
             (util/aggregate-string-value memo :what node)

             (= :em (:tag node))
             (set-speaker memo (first (:content node)))

             (= :b (:tag node))
             (let [value (first (:content node))
                   speaker (when (re-find #":" value) (first (string/split value #":")))]
               (if speaker
                 (set-speaker memo speaker)
                 memo))

             (= :a (:tag node))
             (let [{:keys [name href]} (:attrs node)]
               ;; (log/trace (<< "* <a name=\"~{name}\" href=\"~{href}\">"))
               (cond
                (string? name)
                (assoc memo :when (util/irc-to-joda-time year month day name))

                (string? href)
                (util/aggregate-string-value memo :what href)

                :default
                (throw (Exception. (str "I have no idea what this anchor tag is! " node)))))

             :default
             (throw (Exception. (str "I have no idea how to handle this node: " node)))))

          {:who @*last-person*}
          item))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; will return nil for valid nodes which are just empty messages
(defn node-to-comment [year month day node]
  (try
    (let [preprocessed-node (preprocess-node year month day node)
          {:keys [who when what]} preprocessed-node]
      (if (and who when what)
        (Comment. who what when)
        nil))
    (catch Throwable ex
       (println "puked on this node")
       (pp/pprint node)
       (throw ex))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn html-to-comments [url html-data]
  (let [[year month day] (util/grab-log-date url)]
    (remove nil? (map #(node-to-comment year month day (:content %))
                      (html/select html-data [:#main :p])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-url
  "Return a set of Enlive nodes from the source at url"
  [url]
  (try (html/html-resource (java.net.URL. url))
       (catch java.io.FileNotFoundException e
         {})))

(load "core_csv")
(load "core_sql")

(defn persist-nodes
  "Returns map of functions for different kinds of persistence, e.g. csv, sql, etc."
  [persistence-type]
  (condp = persistence-type
      :csv nodes-to-csv-file
      :mysql nodes-to-mysql-db))

;; :while (not= current-url (util/url-for-date (time/now)))

(defn scrape-log
  [persistence-type destination current-url]
  (let [current-nodes (fetch-url current-url)]
    (println (str "URL: " current-url))
    ((persist-nodes persistence-type) destination current-url current-nodes)))

;; :while (not= current-url (util/url-for-date (time/now)))

(defn scrape-all-logs
  "Scrape all Clojure IRC logs on the n01se.net. Do not re-scrape a page for which we already have generated a CSV file."
  [persistence-type]
  (doseq [current-url (take 2 (iterate util/calc-next-day-url *start-url*))]
    (if (= persistence-type :csv)
      (let [final-path (->> (util/url-parts current-url)
                            second
                            (re-find #"([^\.]+).html")
                            second)
            destination (-> (str "cache/" final-path ".csv")
                           io/as-file)]
        (when-not (.exists destination)
          (scrape-log persistence-type destination current-url)))
      (scrape-log persistence-type nil current-url))))
