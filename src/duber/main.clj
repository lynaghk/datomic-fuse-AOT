(ns duber.main
  (:require [datomic.api :as d])
  (:import java.io.File
           net.fusejna.ErrorCodes
           net.fusejna.util.FuseFilesystemAdapterFull))

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



(defn -main [& args]

  ;;when this block runs, it magically fixes the problem with fulltext query from the FUSE thread.
  (when (= "work" (first args))
    (println "invoking a fulltext query against an in-memory database")
    (let [conn (new-datomic-conn)]
      (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                    (d/db conn)))))
  

  (let [conn (new-datomic-conn)
        testmount (doto (java.io.File. "testmount/")
                    (.mkdirs))
        ;;A proxy with a getattr method for FUSE thread to invoke
        f (proxy [FuseFilesystemAdapterFull] []
            (getattr [path stat]
              (try
                (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                              (d/db conn)))
                (catch Throwable e
                  (.printStackTrace e)))

              ;;since this is just a test to get the FUSE thread to invoke our proxy, we can always return "nothing found"
              (- (ErrorCodes/ENOENT))))]

    ;;mount our filesystem
    (.mount f testmount false)
    ;;this will force `getattr` to be called in our proxy method
    (.exists testmount))

  (System/exit 0))