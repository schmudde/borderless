(ns borderless.core
  (:gen-class)
  (:require [borderless.osc-server :as osc]))

(defn boot-server []
    (println "booting server")
    (osc/person-enter)
    (osc/person-leave)
    (osc/person-updated))

(defn -main
  [& args]
  (println "We are ready__")
  (boot-server))
