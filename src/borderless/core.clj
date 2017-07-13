(ns borderless.core
  (:gen-class)
  (:require [overtone.core :refer [boot-external-server]]))

(defn boot-server! []
  (boot-external-server)
  (println "booting server")
  (require '[borderless.osc-server :as osc]))

(defn -main
  [& args]
  (boot-server!))
