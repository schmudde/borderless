(ns borderless.core
  (:gen-class)
  (:require [borderless.osc-server :as osc]))

(defn boot-server []
  (println "hello world!")
;  (osc/person-enter)
;  (osc/person-leave)
;  (osc/person-updated)
  )

(defn -main
  [& args]
  (println "Hello, World!"))
