(ns duber.main
  (:require [datomic.api :as d])
  (:gen-class))

(definterface IFoo
  (^int foo [x]))

(defn -main [& args]

  (let [uri (str "datomic:mem://" (gensym))
        _ (d/create-database uri)
        conn (d/connect uri)
        f (proxy [IFoo] []
            (foo [x]

              (println "running foo")

              (println @(d/transact conn
                                    [{:db/doc "A name"
                                      :db/ident :duber/name
                                      :db/valueType :db.type/string
                                      :db/cardinality :db.cardinality/one
                                      :db/fulltext true
                                      :db/id #db/id[:db.part/db]
                                      :db.install/_attribute :db.part/db}]))

              (println @(d/transact conn
                                    [{:db/id #db/id[:db.part/user]
                                      :duber/name "hello world"}]))

              (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                            (d/db conn)))


              1))]

      (.run (Thread. (fn []
                       (println "in a thread")
                       (.foo f "hmm."))))
    (System/exit 0)))