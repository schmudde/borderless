(defproject borderless "0.2"
  :description "Borderless: Generative Audio Experience"
  :url "http://borderless.online"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [environ "1.0.3"]
                 [overtone "0.10.2"]]
  :plugins [[lein-environ "1.0.3"]
            [lein-kibit "0.1.5"]]
  :jvm-opts ^:replace []
  :min-lein-version "2.6.1"

  :source-paths ["src"]
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

;; TODO: Currently on Overtone 0.10.1 because of the issue here: https://github.com/overtone/overtone/commit/320f97093d5e2861eaf36d6ef0dcb8a3a5fc6a25
