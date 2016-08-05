(ns borderless.core
  (:gen-class)
  (:use overtone.live))
;;  (:require [overtone.live]))
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


(defn tremelo [] (sin-osc:kr 0.5))


;; need to pass the ugen to something that can execute it and abstract it away to eventually add reverb

(definst drone-aw [freq 100]
  "Inst calls the synth macro which takes a synthesizer definition form. The saw function represents a unit-generator, or ugen. These are the basic building blocks for creating synthesizers, and they can generate or process both audio and control signals (odoc saw)"
  (* (env-gen (lin :sustain 2) 1 1 0 1 FREE)
     (+ (resonz
      (saw (+ freq (sin-osc:kr 0.5)))
      570
      0.1)
     (resonz
      (saw (+ freq (sin-osc:kr 0.5)))
      840
      0.1)
     (resonz
      (saw (+ freq (sin-osc:kr 0.5)))
      2410
      0.1))))


(definst drone-eh [freq 100]
  "Inst calls the synth macro which takes a synthesizer definition form. The saw function represents a unit-generator, or ugen. These are the basic building blocks for creating synthesizers, and they can generate or process both audio and control signals (odoc saw)"
  (* (env-gen (lin :sustain 2) 1 1 0 1 FREE)
     (+ (resonz
      (saw (+ freq (sin-osc:kr 0.5)))
      530
      0.1)
     (resonz
      (saw (+ freq (sin-osc:kr 0.5)))
      1840
      0.1)
     (resonz
      (saw (+ freq (sin-osc:kr 0.5)))
      2480
      0.1))))




;; (defn ramp [tone ]
;;   (ctl drone :freq

;;;;;;;;;;;;;;;;;;;;;
;; OSC Server      ;;
;;;;;;;;;;;;;;;;;;;;;

;; (def PORT 12000)

;; (def server (osc-server PORT))
;; (def client (osc-client "localhost" PORT))

;; (defn close-down! []
;;   ;; remove handler
;;   (osc-rm-handler server "/TSPS/personEntered")

;;   ;; stop listening and deallocate resources
;;   (osc-close server))

;; (def noise-chooser
;;   (fn [] (rand-nth noise)))

;; (defn person-entered []
;;   (osc-handle server "/TSPS/personEntered"
;;               (fn [msg]
;;                 ((noise-chooser))
;;                 (println "Handler for /TSPS/personEntered: " msg))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
