(defproject borderless "0.1.0-SNAPSHOT"
  :description "Borderless generative audio example"
  :url "http://beyondthefra.me/bordrless"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha4"] ;; alpha 5+ does not seem to work
                 [overtone "0.10.1"]]
  :main ^:skip-aot borderless.core
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}
             :uberjar {:aot :all}})
