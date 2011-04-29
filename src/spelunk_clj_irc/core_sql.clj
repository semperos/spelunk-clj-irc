(in-ns 'spelunk-clj-irc.core)

(defn escape-sql-q
  "Escape single quotes for SQL input"
  [s]
  (string/replace s #"'" "''"))

(extend-type Comment
  ISQLSerializable
  (toSQL [self]))

(defn nodes-to-sql
  [url nodes]
  (let [comments (html-to-comments url nodes)]
    (map #(toSQL %) comments)))

(defn nodes-to-sql-file)