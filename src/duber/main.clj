(ns duber.main
  (:require [datomic.api :as d])
  (:import duber.Fooer)
  (:gen-class))

(defn new-datomic-conn
  []
  (let [uri (str "datomic:mem://" (gensym))
        _ (d/create-database uri)
        conn (d/connect uri)]

    @(d/transact conn
                 [{:db/doc "A name"
                   :db/ident :duber/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/fulltext true
                   :db/id #db/id[:db.part/db]
                   :db.install/_attribute :db.part/db}])

    @(d/transact conn
                 [{:db/id #db/id[:db.part/user]
                   :duber/name "hello world"}])
    conn))

(defn fooer [!db]
  (proxy [Fooer] []
    (foo []

      (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                    @!db))

      (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                    @!db)))))

(defn -main [& args]

  (let [conn (new-datomic-conn)]
    (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                  (d/db conn)))

    (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                  (d/db conn))))

  (let [conn (new-datomic-conn)
        f (fooer (atom (d/db conn)))]
    (.foo f))

  (System/exit 0))