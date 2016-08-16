(ns borderless.core
  (:gen-class)
  (:require [borderless.osc-server :as osc]))


(defn boot-server []
  (osc/person-enter)
  (osc/person-leave)
  (osc/person-updated)
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
