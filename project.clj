(defproject spelunk-clj-irc "0.0.1-SNAPSHOT"
  :description "Some code to screenscrape and analyze the Clojure IRC logs provided by n01se.net"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [enlive "1.0.0"]
                 [clj-time "0.3.0"]
                 [mysql/mysql-connector-java "5.1.16"]
                 [org.mongodb/mongo-java-driver "2.5"]
                 [congomongo "0.1.4-SNAPSHOT"] ; download and compile manually
                 [incanter/incanter-core "1.2.3"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]])
