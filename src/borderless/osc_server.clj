(ns borderless.osc-server
  (:use overtone.osc)
  (:require [borderless.sound :as sound]))

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

;; (osc-handle server "/test" (fn [msg] (println "MSG: " msg)))
;; Register a handler function for the /test OSC address
;; The handler takes a message map with the following keys:
;;   [:src-host, :src-port, :path, :type-tag, :args]

(defn person-updated []
  (osc-handle server "/TSPS/personUpdated"
              (fn [msg]
                ;;(println msg)
                (sound/control-foo (nth (:args msg) 2)) ;; Send OSC 2: "age" of the person
                )))

(defn person []
  "Full Response Example - {:path /TSPS/personEntered/, :type-tag iiifffffffffffffffff, :args (409 0 2 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0), :src-host localhost, :src-port 49551}"
  (sound/control-foo (nth  '(409 0 20 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0) 2))
)

;; Let's now override our boring "/1/fader6" handler which just prints out the incoming message to instead extract the slider's val and call control-foo: (osc-handle server "/1/fader6" (fn [msg] (control-foo (first (:args msg)))))

(defn plus [x y] (+ x y))


;; (def noise-chooser
;;   (fn [] (rand-nth noise)))

;; (defn person-entered []
;;   (osc-handle server "/TSPS/personEntered"
;;               (fn [msg]
;;                                         ;((noise-chooser))
;;                 (println msg)
;;                 msg)))
