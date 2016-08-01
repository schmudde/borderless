(ns borderless.core
  (:gen-class)
  (:use overtone.live))
;  (:require [overtone.live]))
;; Why can't I use require?!?

(def PORT 12001)

                                         ; start a server and create a client to talk with it
(def server (osc-server PORT))
(def client (osc-client "localhost" PORT))

(defn close-down! []
  ;; remove handler
  (osc-rm-handler server "/TSPS/personEntered")

  ;; stop listening and deallocate resources
  (osc-close server))


(defn testers!
  "Side effect sound"
  []
  (demo (sin-osc)))

(defn person-entered []
  (osc-handle server "/TSPS/personEntered" (fn [msg]
                                             (testers!)
                                             (println "Handler for /TSPS/personEntered: " msg))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
