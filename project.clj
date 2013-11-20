(defproject duber "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.8.4270"]
                 
                 [fuse-jna "abceef"]
                 [net.java.dev.jna/jna "3.5.2"] ;;dependency for fuse-jna
                 ]

  :repositories {"local" {:url "file://repo"}}
  :jvm-opts ["-Djna.nosys=true"]
  :main duber.main)
