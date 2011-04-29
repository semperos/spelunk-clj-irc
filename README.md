# spelunk-clj-irc

[n01se.net](http://clojure-log.n01se.net/) has been graciously capturing and hosting HTML copies of [Clojure IRC](irc://irc.freenode.net/#clojure) logs. This code intends to screenscrape said logs, convert to simpler formats, and possibly even answer simple questions about the data.

## Usage

The main function of interest is `scrape-all-logs` located in the `spelunk-clj-irc.core` namespace. Pass in the way in which you want the logs saved, and it will scrape all of the logs hosted on the site, starting with February 1st, 2008.

    (use 'spelunk-clj-irc.core)
    (scrape-all-logs :csv)

At this time, the only other option is `:mysql`, which requires a little configuration ahead-of-time. Look at the `src/spelunk_clj_irc/core_mysql.clj` file for details and for some helpful functions to get you started.

## License

Copyright (C) 2010 Seth Schroeder

Distributed under the Eclipse Public License, the same as Clojure.
