# spelunk-clj-irc

[n01se.net](http://clojure-log.n01se.net/) has been graciously capturing and hosting HTML copies of [Clojure IRC](irc://irc.freenode.net/#clojure) logs. This code intends to screenscrape said logs, convert to simpler formats, and possibly even answer simple questions about the data.

## Usage

The main function of interest is `scrape-all-logs` located in the `spelunk-clj-irc.core` namespace. Pass in the way in which you want the logs saved, and it will scrape all of the logs hosted on the site, starting with February 1st, 2008.

    (use 'spelunk-clj-irc.core)
    (scrape-all-logs :csv)

At this point, the only type of persistence that is supported is CSV files, one per day of logs. Next in line are plain SQL files and then hopefully a MongoDB persistence option.

## License

Copyright (C) 2010 Seth Schroeder

Distributed under the Eclipse Public License, the same as Clojure.
