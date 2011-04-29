(in-ns 'spelunk-clj-irc.core)

(def mysql-host "localhost")
(def mysql-port 3306)
(def mysql-db-name "clj_irc")
(def mysql-log-tbl :logs)

(def mysql-db {:classname "com.mysql.jdbc.Driver"
               :subprotocol "mysql"
               :subname (str "//" mysql-host ":" mysql-port "/" mysql-db-name)
               :user "root"
               :password ""})

(defn mysql-create-logs-table
  "CREATE TABLE the logs table"
  []
  (sql/with-connection mysql-db
    (sql/create-table mysql-log-tbl
                      [:id :integer "PRIMARY KEY" "AUTO_INCREMENT"]
                      [:who "varchar(255)"]
                      [:what :text]
                      [:when_dt :datetime])))

(defn mysql-clean-logs-table
  "TRUNCATE the logs table"
  []
  (sql/with-connection mysql-db
    (sql/do-commands (str "TRUNCATE " (name mysql-log-tbl)))))

(defn mysql-drop-logs-table
  "DROP the logs table"
  []
  (sql/with-connection mysql-db
    (try
      (do
        (sql/drop-table mysql-log-tbl))
      (catch Exception _))))

(def insert-query (str "INSERT INTO " (name mysql-log-tbl) " (who, what, when_dt) "
                       "VALUES (?, ?, ?)"))

(defn nodes-to-mysql-db
  [_ url nodes]
  (sql/with-connection mysql-db
    (let [comments (html-to-comments url nodes)]
      (doseq [comment comments]
        (sql/do-prepared insert-query
                         [(:who comment)
                          (:what comment)
                          (date/unparse util/mysql-dt-formatter (:when comment))])))))

