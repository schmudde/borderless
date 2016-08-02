(ns borderless.core
  (:gen-class)
  (:use overtone.live))
;  (:require [overtone.live]))
;; Why can't I use require?!?

;; (volume 20)

;;;;;;;;;;;;;;;;;;;;;
;; Audio           ;;
;;;;;;;;;;;;;;;;;;;;;


(def eh (sample "~/work/code/borderless/resources/audio/eh.aif"))

(def noise-1 (sample "~/work/code/borderless/resources/audio/Test21-1.aif"))
(def noise-2 (sample "~/work/code/borderless/resources/audio/Test21-2.aif"))
(def noise-3 (sample "~/work/code/borderless/resources/audio/Test21-3.aif"))

(def noise [noise-1 noise-2 noise-3])

(def eh-buf (load-sample "resources/audio/eh.aif"))

(defsynth reverb-on-left []
  (let [dry (play-buf 1 eh-buf)
    wet (free-verb dry 1)]
    (out 0 [wet dry])))

(defn testers!
  "Side effect sound"
  []
  (demo (sin-osc)))

;;;;;;;;;;;;;;;;;;;;;
;; OSC Server      ;;
;;;;;;;;;;;;;;;;;;;;;

(def PORT 12000)

(def server (osc-server PORT))
(def client (osc-client "localhost" PORT))

(defn close-down! []
  ;; remove handler
  (osc-rm-handler server "/TSPS/personEntered")

  ;; stop listening and deallocate resources
  (osc-close server))

(def noise-chooser
  (fn [] (rand-nth noise)))

(defn person-entered []
  (osc-handle server "/TSPS/personEntered"
              (fn [msg]
                ((noise-chooser))
                (println "Handler for /TSPS/personEntered: " msg))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
