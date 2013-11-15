(ns duber.main
  (:require [datomic.api :as d])
    (:import java.io.File
           (net.fusejna DirectoryFiller
                        ErrorCodes
                        FuseFilesystem
                        StructFuseFileInfo$FileInfoWrapper
                        StructStat$StatWrapper)
           net.fusejna.types.TypeMode$NodeType
           net.fusejna.types.TypeMode$ModeWrapper
           net.fusejna.util.FuseFilesystemAdapterFull)
  (:gen-class))

(defn mkfs [db]
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

  (let [uri (str "datomic:mem://" (gensym))
        _ (d/create-database uri)
        conn (d/connect uri)]
    
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

        (.mount (mkfs (d/db conn))
                (doto (java.io.File. "foo/")
                  (.mkdirs))
                false))

  ;;(System/exit 0)
  )