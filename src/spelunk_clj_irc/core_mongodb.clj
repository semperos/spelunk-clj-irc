(in-ns 'spelunk-clj-irc.core)

(def mongodb-coll :logs)

(defn nodes-to-mongodb
  [_ url nodes]
  (let [comments (html-to-comments url nodes)]
    (doseq [comment comments]
      (mongo/insert! mongodb-coll
                     (into {} (assoc comment :when (.toDate (:when comment))))))))

(comment

  (mongo/mongo! :db "clj_irc")

  )