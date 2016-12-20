(defproject borderless "0.2"
  :description "Borderless: Generative Audio Experience"
  :url "http://borderless.online"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [overtone "LATEST"]
                 [environ "1.0.3"]]
  :plugins [[lein-environ "1.0.3"]
            [lein-git-deps "0.0.1-SNAPSHOT"]]
  :git-dependencies [["https://github.com/overtone/overtone.git"]]

  :jvm-opts ^:replace []
  :min-lein-version "2.6.1"

  :source-paths ["src" ".lein-git-deps/overtone/src"]
  :test-paths ["test/clj"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :uberjar-name "borderless.jar"

  :main borderless.core
  :profiles {:dev
             {:dependencies [
                             [org.clojure/tools.nrepl "0.2.12"]
                             [org.clojure/test.check "0.9.0"]]
              :source-paths ["dev"]
              }
             :uberjar
             {:source-paths ^:replace ["src"]
              :hooks []
              :omit-source true
              :aot :all}})
