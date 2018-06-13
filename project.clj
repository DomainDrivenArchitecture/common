(defproject dda/pallet-common "0.5.0-SNAPSHOT"
  :description "Common functions used across pallet projects"
  :url "http://palletops.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]])
