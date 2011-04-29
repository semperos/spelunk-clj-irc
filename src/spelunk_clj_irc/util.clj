;   Copyright (c) Seth Schroeder. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;   Source of IRC logs: http://clojure-log.n01se.net/

(ns spelunk-clj-irc.util
  (:require [clj-time.format :as date]
            [clj-time.core :as time]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as html]))

(def _time-pattern #"^(\d\d):(\d\d)(.)?$")

(def _date-pattern
     #"^.+- (\p{Alpha}{3})\p{Space}(\p{Digit}{2})\p{Space}+(\p{Digit}{4})$")

(def _formatter (date/formatter "yyyy MMM DD HH mm ss"))
(def mysql-dt-formatter (date/formatter "yyyy-MM-dd HH:mm:ss"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare url-parts)
(defn grab-log-date
  "Given url, parse out log date"
  [url]
  (let [final-path (second (url-parts url))]
    (string/split (second (re-find #"([^\.]+).html" final-path)) #"-")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; handles all of the following:
;; hh : hours
;; mm : minutes
;; a-z? : a character a-z, sometimes. Don't blame me for the data format.
(defn normalize-time-component [hhmma-z?]
  (let [act_val (first (drop-while nil? [hhmma-z? "a"]))]
    (if (re-matches #"^\d\d$" act_val)
      act_val
      ;; seconds aren't available in the data, and synthesizing them seems risky.
      ;; might be better to increment seconds per message per minute...
      #_(format "%02d" (- (.hashCode act_val) (.hashCode "a")))
      "00")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This is probably stupidly expensive. Also smells like refactoring.
;; um, what? (date/parse (date/formatter "yyyy MMM DD") "2008 Mar 08")
;; #<DateTime 2008-01-08T00:00:00.000Z>
;; after identifying that problem, get rid of this abomination:
(defn irc-to-joda-time [year month day hhmma-z]
  (let [[hour minute second] (map normalize-time-component
                                  (rest (first (re-seq _time-pattern hhmma-z))))]
    (apply time/date-time
           (map #(Integer/parseInt %) [year month day hour minute second]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; this smells like refactoring... string/join is awkward
(defn aggregate-string-value [hash key in-str]
  (let [prev-str (get hash key)
        cur-str  (when (string? in-str) (string/trim in-str))
        strs     (remove string/blank? [prev-str cur-str])]
    (assoc hash key (case (count strs)
                          2 (string/join " " strs)
                          1 (first strs)
                          0 nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn url-parts
  "Return the body (everything up to the final path) and the tail (the final path) of a url, e.g. http://example.com/foo/bar would return [\"http://example.com/foo\", \"bar\"]"
  [path]
  (let [matches (re-find #"(.*?)/([^/]*)$" path)
        body (nth matches 1)
        tail (nth matches 2)]
    [body tail]))

(defn url-for-date
  "Generate a string URL for a log page on n01se.net given a particular date"
  [dt]
  (str "http://clojure-log.n01se.net/date/" (date/unparse (date/formatters :year-month-day) dt) ".html"))

(defn calc-next-day-url
  "'Increment' a url to get the next day's url"
  [url]
  (let [fmt (date/formatters :year-month-day)
        [body tail] (url-parts url)
        this-url-date (->> (re-find #"([^\.]+).html" tail)
                           second
                           (date/parse fmt))
        next-date-str (->> (time/days 1)
                           (time/plus this-url-date)
                           (date/unparse fmt))]
    (str body "/" next-date-str ".html")))