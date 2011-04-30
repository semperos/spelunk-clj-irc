(defproject spelunk-clj-irc "0.0.1-SNAPSHOT"
  :description "Some code to screenscrape and analyze the Clojure IRC logs provided by n01se.net"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [enlive "1.0.0"]
                 [clj-time "0.3.0"]
                 [mysql/mysql-connector-java "5.1.16"]
                 [congomongo "0.1.3-SNAPSHOT"]
                 [incanter "1.2.3"]
                 [incanter/incanter-mongodb "1.2.3"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]])
