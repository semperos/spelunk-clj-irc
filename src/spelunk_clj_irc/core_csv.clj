(in-ns 'spelunk-clj-irc.core)

(extend-type Comment
  ICSVSerializable
  (getHeader [_] "\"who\",\"what\",\"when\"")
  (toCSV [self]
    (string/join "," (map #(str \"
                                (string/replace (str (% self)) #"\"" "\"\"")
                                \")
                          [:who :what :when]))))

(defn nodes-to-csv [url nodes]
  (let [comments (html-to-comments url nodes)]
    (map #(toCSV %) comments)))

(defn nodes-to-csv-file [destination url nodes]
  (let [csv-data (nodes-to-csv url nodes)]
    (when-not (or (.exists destination) (empty? csv-data))
      (println url "   =>   " destination)
      (with-open [os (io/output-stream destination)]
        (.write os (.getBytes (str (getHeader (Comment. nil nil nil)) "\n")))
        (doseq [row csv-data]
          (.write os (.getBytes (str row "\n"))))))))