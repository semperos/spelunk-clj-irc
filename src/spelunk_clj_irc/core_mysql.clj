(in-ns 'spelunk-clj-irc.core)

(def mysql-host "localhost")
(def mysql-port 3306)
(def mysql-db-name "clj_irc")
(def mysql-tbl :logs)

(def mysql-db {:classname "com.mysql.jdbc.Driver"
               :subprotocol "mysql"
               :subname (str "//" mysql-host ":" mysql-port "/" mysql-db-name)
               :user "root"
               :password ""})

(defn mysql-create-logs-table
  "CREATE TABLE the logs table"
  []
  (sql/with-connection mysql-db
    (sql/create-table mysql-tbl
                      [:id :integer "PRIMARY KEY" "AUTO_INCREMENT"]
                      [:who "varchar(255)"]
                      [:what :text]
                      [:when_dt :datetime])))

(defn mysql-clean-logs-table
  "TRUNCATE the logs table"
  []
  (sql/with-connection mysql-db
    (sql/do-commands (str "TRUNCATE " (name mysql-tbl)))))

(defn mysql-select-id-by-date
  "Selects first record by date `dt`"
  [dt]
  (sql/with-connection mysql-db
    (sql/with-query-results rs
      [(str "SELECT id FROM " (name mysql-tbl) " WHERE "
            "YEAR(when_dt)  = '" (time/year dt) "' AND "
            "MONTH(when_dt) = '" (time/month dt) "' AND "
            "DAY(when_dt)   = '" (time/day dt) "' LIMIT 1")]
      (:id (first rs)))))

(defn mysql-clean-logs-by-date
  "Clean log records that have a date/time greater than or equal to the date `dt`"
  [dt]
  (let [start-id (mysql-select-id-by-date dt)]
   (sql/with-connection mysql-db
     (sql/delete-rows mysql-tbl ["id>=?" start-id]))))

(defn mysql-drop-logs-table
  "DROP the logs table"
  []
  (sql/with-connection mysql-db
    (try
      (do
        (sql/drop-table mysql-tbl))
      (catch Exception _))))

(def insert-query (str "INSERT INTO " (name mysql-tbl) " (who, what, when_dt) "
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

