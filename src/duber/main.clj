(ns duber.main
  (:require [datomic.api :as d])
  (:import duber.Fooer
           java.io.File
           (net.fusejna DirectoryFiller
                        ErrorCodes
                        FuseFilesystem
                        StructFuseFileInfo$FileInfoWrapper
                        StructStat$StatWrapper)
           net.fusejna.types.TypeMode$NodeType
           net.fusejna.types.TypeMode$ModeWrapper
           net.fusejna.util.FuseFilesystemAdapterFull)
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


(defn mkfs-conn
  [conn]
  (proxy [FuseFilesystemAdapterFull] []

    (getattr
      [^String path ^StructStat$StatWrapper stat]
      (prn "getattr" path)
      (.setMode stat TypeMode$NodeType/DIRECTORY)
      0)

    (readdir
      [^String path ^DirectoryFiller filler]
      (prn "reading directory")
      (let [^Iterable entries (->> (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                                        (d/db conn))
                                   first)]
        (.add filler entries))
      0)))

(defn mkfs-db
  [db]
  (proxy [FuseFilesystemAdapterFull] []

    (getattr
      [^String path ^StructStat$StatWrapper stat]
      (prn "getattr" path)
      (.setMode stat TypeMode$NodeType/DIRECTORY)
      0)

    (readdir
      [^String path ^DirectoryFiller filler]
      (prn "reading directory")
      (let [^Iterable entries (->> (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                                        db)
                                   first)]
        (.add filler entries))
      0)))





(defn -main [& args]

  (println "plain query")
  (let [conn (new-datomic-conn)]
    (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                  (d/db conn)))

    (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                  (d/db conn))))

  (println "query running in proxy inheriting from concrete Java class")
  (let [conn (new-datomic-conn)
        f (proxy [Fooer] []
            (foo []
              (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                            (d/db conn)))

              (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                            (d/db conn)))))]
    (.foo f))

  (println "query running in proxy inheriting from concrete Java class, db outside of proxy")
  (let [conn (new-datomic-conn)
        db (d/db conn)
        f (proxy [Fooer] []
            (foo []
              (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                            db))

              (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                            db))))]
    (.foo f))





  

  
  (println "query running in mounted proxy inheriting from fuse-jna, inline conn")
  (let [conn (new-datomic-conn)
        f   (proxy [FuseFilesystemAdapterFull] []

              (getattr
                [^String path ^StructStat$StatWrapper stat]
                (prn "getattr" path)
                (.setMode stat TypeMode$NodeType/DIRECTORY)
                0)

              (readdir
                [^String path ^DirectoryFiller filler]
                (prn "reading directory")
                (let [^Iterable entries (->> (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                                                  (d/db conn))
                                             first)]
                  (.add filler entries))
                0))]
    
    (.mount f
            (doto (java.io.File. "fooconn/")
              (.mkdirs))
            false))





  

  (println "query running in mounted proxy inheriting from fuse-jna, defn conn")
  (let [conn (new-datomic-conn)
        f  (mkfs-conn conn)]
    
    (.mount f
            (doto (java.io.File. "fooconndef/")
              (.mkdirs))
            false))


  (println "query running in mounted proxy inheriting from fuse-jna inline db")
  (let [conn (new-datomic-conn)
        db (d/db conn)
        f   (proxy [FuseFilesystemAdapterFull] []

              (getattr
                [^String path ^StructStat$StatWrapper stat]
                (prn "getattr" path)
                (.setMode stat TypeMode$NodeType/DIRECTORY)
                0)

              (readdir
                [^String path ^DirectoryFiller filler]
                (prn "reading directory")
                (let [^Iterable entries (->> (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                                                  db)
                                             first)]
                  (.add filler entries))
                0))]
    (.mount f
            (doto (java.io.File. "foodb/")
              (.mkdirs))
            false))

  (println "query running in mounted proxy inheriting from fuse-jna, defn db")
  (let [conn (new-datomic-conn)
        f  (mkfs-db (d/db conn))]
    
    (.mount f
            (doto (java.io.File. "foodbdef/")
              (.mkdirs))
            false))





;;  (System/exit 0)
  )