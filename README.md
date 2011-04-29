# spelunk-clj-irc

[n01se.net](http://clojure-log.n01se.net/) has been graciously capturing and hosting HTML copies of [Clojure IRC](irc://irc.freenode.net/#clojure) logs. This code intends to screenscrape said logs, convert to simpler formats, and possibly even answer simple questions about the data.

## Usage

The main function of interest is `scrape-logs` located in the `spelunk-clj-irc.core` namespace. Pass in the way in which you want the logs saved, and it will scrape all of the logs hosted on the site, starting with February 1st, 2008.

    (use 'spelunk-clj-irc.core)
    (scrape-logs :csv)

You can optionally pass a start and end date (as instances of Joda DateTime) to limit the scope of the scrape.

### CSV

If you use the code above `(scrape-logs :csv)`, all log files will be scraped and saved as individual CSV files into a `cache` directory, one per day of logs.

### MySQL

Use the `:mysql` option as follows:

    (use 'spelunk-clj-irc.core)
    (scrape-logs :mysql)

In order to use the MySQL method, you must have already created a MySQL database. Alter the configuration options in the `src/spelunk_clj_irc/core_mysql.clj` file to match your local configuration (database name, user, password, etc.). Once the database has been created, you can run the above function to have all logs written to the database. Each entry in the logs (a person saying one thing at a particular time) is saved as a row in the database.

See the `core_mysql.clj` file for some utility functions for managing the database. You can use them to clear out portions of the logs table, and then run `(scrape-logs :mysql start-date)` to do incremental scrapes based on the date (at this time, the granularity is by day).

### MongoDB

Use the `:mongodb` option as follows:

    (use 'spelunk-clj-irc.core)
    (use 'somnium.congomongo)
    (mongo! :db "clj_irc")
    (scrape-logs :mongodb)

This first prepares a MongoDB database called `clj_irc` and then does a full scrape of the logs. Like the MySQL option, this will save each record of the log as a separate document in a collection called `logs`. You can use any functions provided by the `congomongo` library to manipulate the data from Clojure, or use the MongoDB shell that comes with MongoDB.

## License

Copyright (C) 2010 Seth Schroeder

Distributed under the Eclipse Public License, the same as Clojure.
