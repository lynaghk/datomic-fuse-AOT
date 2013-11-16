# duber

I created this tiny project to try and isolate AOT compilation problems on a larger FUSE + Datomic project of mine.
Discussion on datomic mailing list: https://groups.google.com/forum/#!topic/datomic/8WjnKqSrjgg

Run

    lein uberjar && java -Djna.nosys=true -jar target/duber-0.1.0-SNAPSHOT-standalone.jar

to get this exception:

```
"reading directory"
JNA: Callback net.fusejna.StructFuseOperations$37@46aecc14 threw the following exception:
com.google.common.util.concurrent.UncheckedExecutionException: java.lang.ClassNotFoundException: clojure.lang.RT, compiling:(NO_SOURCE_PATH:0:0)
        at com.google.common.cache.LocalCache$Segment.get(LocalCache.java:2263)
        at com.google.common.cache.LocalCache.get(LocalCache.java:4000)
        at com.google.common.cache.LocalCache.getOrLoad(LocalCache.java:4004)
        at com.google.common.cache.LocalCache$LocalLoadingCache.get(LocalCache.java:4874)
        at datomic.cache$fn__897.invoke(cache.clj:68)
        at datomic.cache$fn__884$G__879__891.invoke(cache.clj:60)
        at datomic.cache.WrappedGCache.valAt(cache.clj:99)
        at clojure.lang.RT.get(RT.java:645)
        at datomic.query$q.invoke(query.clj:448)
        at datomic.api$q.doInvoke(api.clj:31)
        at clojure.lang.RestFn.invoke(RestFn.java:423)
        at duber.main$mkfs_db$fn__19.invoke(main.clj:49)
        at duber.main.proxy$net.fusejna.util.FuseFilesystemAdapterFull$0.readdir(Unknown Source)
```

It took me several hours to consistently replicate.
(See the history of this repo.)
The exception is only thrown when a datomic fulltext query is run within a `proxy` invoked by a FUSE callback.
Note that if you exercise the Datomic fulltext codez by running:

```clojure
(let [conn (new-datomic-conn)]
  (println (d/q '[:find ?n :where [?e :duber/name ?n]]
                (d/db conn)))

  (println (d/q '[:find ?n :where [(fulltext $ :duber/name "hello") [[?e ?n]]]]
                (d/db conn))))
```

*before* mounting the `proxy` then everything works fine.
(See `src/duber/main.clj`.)

All tests were run with Clojure 1.5.1 and datomic free 0.8.4218.

On OS X 10.7.5 with

```
java version "1.7.0_10"
Java(TM) SE Runtime Environment (build 1.7.0_10-b18)
Java HotSpot(TM) 64-Bit Server VM (build 23.6-b04, mixed mode)
```

and on x64 Debian Linux with

```
java version "1.7.0_25"
OpenJDK Runtime Environment (IcedTea 2.3.12) (7u25-2.3.12-4)
OpenJDK 64-Bit Server VM (build 23.7-b01, mixed mode)
```

 

Everything works fine from a REPL.
