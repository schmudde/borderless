(ns borderless.core
  (:gen-class)
  (:require [overtone.core :refer [boot-external-server connect-external-server boot-internal-server]]))

(defn boot-server! []
  (boot-internal-server)
  (println "booting server")
  (require '[borderless.osc-server :as osc]))

(defn -main
  [& args]
  (boot-server!))
