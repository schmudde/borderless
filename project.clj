(defproject borderless "0.1.0-SNAPSHOT"
  :description "Borderless generative audio example"
  :url "http://beyondthefra.me/bordrless"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [overtone "0.10.1"]
                 [org.clojure/clojurescript "1.8.40"]]
  :cljsbuild {:source-path "src"
                           :compiler {:output-dir "resources/public/cljs"
                                      :output-to "resources/public/cljs/bootstrap.js"
                                      :optimizations :simple
                                      :pretty-print true}}
  :main ^:skip-aot borderless.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
