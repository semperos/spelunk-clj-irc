;;; ## Stats for Clojure's IRC Logs
;;;
;;; Warning: Less-than-functional code ahead.
;;;
;;; In order to provide a meaningful set of functions,
;;; the functions in this namespace assume that the MongoDB collection's
;;; fields are `who`, `what` and `when`, to match the structure of the Comment
;;; record. You can redefine the var's if you setup your collection differently.
;;;
;;; When you call these functions, you should first define a db
;;; connection with the `make-connection` function, and then wrap
;;; the target function call in a `with-mongo` form. For example:
;;;
;;;     (def db (mongo/make-connection "clj_irc"))
;;;     (mongo/with-mongo db
;;;       (total-comments :logs))

(ns spelunk-clj-irc.stats.core
  (:require [somnium.congomongo :as mongo]
            [spelunk-clj-irc.util :as util])
  (:import java.text.SimpleDateFormat
           java.util.Calendar
           java.util.regex.Pattern
           java.util.Date))


(def who-field  :who)
(def what-field :what)
(def when-field :when)

(defn date-for-day
  "Return string formatted date with granularity of day"
  [d]
  (let [year-month-day (SimpleDateFormat. "yyyy-MM-dd")]
    (.format year-month-day d)))

(defn calc-next-day
  "Given a Java Date object, calculate day"
  [d]
  (let [c (doto (Calendar/getInstance)
            (.setTime d)
            (.add (Calendar/DATE) 1))]
    (.getTime c)))

(defn total-comments*
  "Return total number of IRC messages across all logs. Make sure to setup a connection with `mongo/make-connection` and run this inside `mongo/with-mongo`."
  [mongo-col]
  (mongo/fetch-count mongo-col))
(def total-comments (util/memoize total-comments* (util/ttl-cache-strategy 86400000)))

(defn total-users-absolute*
  "Return total of distinct users that have comment in the IRC channel. This function does not do any intelligent 'merging' of names (appended underscores, back-ticks, etc.)"
  [mongo-col]
  (count (mongo/distinct-values mongo-col (name who-field))))
(def total-users-absolute (util/memoize total-users-absolute* (util/ttl-cache-strategy 86400000)))

(defn total-users-actual*
  "Attempts an intelligent 'merging' of command IRC nick aliases, so a user `foobar` and `foobar_` aren't considered two separate users, for example. This does eliminate users entirely that always use underscores or backticks at the beginning/end of their nicks."
  [mongo-col]
  (let [all-users (mongo/distinct-values mongo-col (name who-field))]
    (count
     (remove (fn [nick] (re-find #"^(_+.*|[^_`]+[_|`]+)$" nick))
             all-users))))
(def total-users-actual (util/memoize total-users-actual* (util/ttl-cache-strategy 86400000)))

(defn total-comments-per-user*
  "Total messages for a user ('intelligent' matching)"
  [mongo-col nick]
  (let [quoted-nick (Pattern/quote nick)]
    (mongo/fetch-count mongo-col
                       :where {:who (re-pattern
                                     (str "^(_*" quoted-nick
                                          "|" quoted-nick "[_|`]*)$"))}
                       :only [:_id])))
(def total-comments-per-user (util/memoize total-comments-per-user* (util/ttl-cache-strategy 86400000)))

(defn total-days-online-per-user*
  "Total days on which a specific user made one or more comment"
  [mongo-col nick]
  (let [quoted-nick (Pattern/quote nick)
        all-dates-for-nick (mongo/fetch mongo-col
                                       :where {:who (re-pattern
                                                     (str "^(_*" quoted-nick
                                                          "|" quoted-nick "[_|`]*)$"))}
                                       :only [:when])
        dates-to-ymd (map #(date-for-day (:when %)) all-dates-for-nick)]
    (count (distinct dates-to-ymd))))
(def total-days-online-per-user (util/memoize total-days-online-per-user* (util/ttl-cache-strategy 86400000)))

(defn total-occurrences-string*
  "Number of times a string occurs in comments in the IRC log"
  [mongo-col s]
  (mongo/fetch-count mongo-col
                     :where {:what (re-pattern (str "(?i)" (Pattern/quote s)))}))
(def total-occurrences-string (util/memoize total-occurrences-string* (util/ttl-cache-strategy 86400000)))

(defn total-occurrences-fn*
  "Number of times a fn is shown in in-code position (preceding paren, or used with map, reduce, apply, filter, remove, comp)"
  [mongo-col a-fn]
  (let [fn-name (name a-fn)
        fn-quoted (Pattern/quote fn-name)
        m (re-pattern (str "(" "\\("               "|"
                               "\\(map\\s+"        "|"
                               "\\(reduce\\s+"     "|"
                               "\\(apply\\s+"      "|"
                               "\\(filter\\s+"     "|"
                               "\\(remove\\s+"     "|"
                               "\\(partial[^\\)]+" "|"
                               "\\(comp[^\\)]+"    "|" ")"
                               fn-quoted))]
    (mongo/fetch-count mongo-col
                       :where {:what m})))
(def total-occurrences-fn (util/memoize total-occurrences-fn* (util/ttl-cache-strategy 86400000)))

(defn average-comments-per-day-per-user*
  "Average comments a user has made per day he/she has been online"
  [mongo-col nick]
  (let [total-comments (total-comments-per-user mongo-col nick)
        total-days (total-days-online-per-user mongo-col nick)]
    (->  (/ total-comments total-days)
         float
         Math/round)))
(def average-comments-per-day-per-user (util/memoize average-comments-per-day-per-user* (util/ttl-cache-strategy 86400000)))