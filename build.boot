(task-options!
 pom  {:project     'sig-gis/gridfire
       :version     "1.0.0"
       :description "SIG's Raster-based Fire Spread and Severity Model"}
 repl {:eval        '(set! *warn-on-reflection* true)
       :init-ns     'gridfire.fire-spread}
 aot  {:namespace   '#{gridfire.cli}}
 jar  {:main        'gridfire.cli})

(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"resources"}
 :dependencies   '[[org.clojure/clojure                 "1.7.0"]
                   [org.clojure/java.jdbc               "0.4.2"]
                   [postgresql/postgresql               "9.3-1102.jdbc41"]
                   [net.mikera/core.matrix              "0.42.0"]
                   [net.mikera/vectorz-clj              "0.36.0"]
                   [org.clojars.lambdatronic/matrix-viz "0.1.7"]])

(deftask build
  "Build my project."
  []
  (comp (aot) (pom) (uber) (jar)))