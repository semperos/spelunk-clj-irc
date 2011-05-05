;   Copyright (c) Seth Schroeder. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;   Source of IRC logs: http://clojure-log.n01se.net/

(ns spelunk-clj-irc.util
  (:refer-clojure :exclude [memoize])
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Thanks Meikel Brandmeyer
(declare naive-strategy)

(defn memoize
  "Returns a memoized version of a referentially transparent function.
  The memoized version of the function keeps a cache of the mapping from
  arguments to results and, when calls with the same arguments are repeated
  often, has higher performance at the expense of higher memory use.

  Optionally takes a cache strategy. The strategy is provided as a map
  containing the following keys. All keys are mandatory!

    - :init   the initial value for the cache and strategy state
    - :cache  access function to access the cache
    - :lookup determines whether a value is in the cache or not
    - :hit    a function called with the cache state and the argument
                list in case of a cache hit
    - :miss   a function called with the cache state, the argument list
                and the computation result in case of a cache miss

  The default strategy is the naive safe-all strategy."
  ([f] (memoize f naive-strategy))
  ([f strategy]
   (let [{:keys [init cache lookup hit miss]} strategy
         cache-state (atom init)
         hit-or-miss (fn [state args]
                       (if (lookup state args)
                         (hit state args)
                         (miss state args (delay (apply f args)))))]
     (fn [& args]
       (let [cs (swap! cache-state hit-or-miss args)]
         (-> cs cache (get args) deref))))))

(def #^{:doc "The naive safe-all cache strategy for memoize."}
  naive-strategy
  {:init   {}
   :cache  identity
   :lookup contains?
   :hit    (fn [state _] state)
   :miss   assoc})

(defn ttl-cache-strategy
  "Implements a time-to-live cache strategy. Upon access to the cache
  all expired items will be removed. The time to live is defined by
  the given expiry time span. Items will only be removed on function
  call. No background activity is done."
  [ttl]
  (let [dissoc-dead (fn [state now]
                      (let [ks (map key (filter #(> (- now (val %)) ttl)
                                                (:ttl state)))
                            dissoc-ks #(apply dissoc % ks)]
                        (-> state
                          (update-in [:ttl]   dissoc-ks)
                          (update-in [:cache] dissoc-ks))))]
    {:init   {:ttl {} :cache {}}
     :cache  :cache
     :lookup (fn [state args]
               (when-let [t (get (:ttl state) args)]
                 (< (- (System/currentTimeMillis) t) ttl)))
     :hit    (fn [state args]
               (dissoc-dead state (System/currentTimeMillis)))
     :miss   (fn [state args result]
               (let [now (System/currentTimeMillis)]
                 (-> state
                   (dissoc-dead now)
                   (assoc-in  [:ttl args] now)
                   (assoc-in  [:cache args] result))))}))
