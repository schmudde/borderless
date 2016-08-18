(ns borderless.sound
  (:use overtone.live))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Person/Sound Mapping ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inst-ns [instrument]
  "This is the instrument name with full namespace qualifiers. Useful becuase the 'sound name' is really the name of the 'definst' macro created by Overtone."
    (eval (symbol "borderless.sound" instrument)))

(def person-sound (atom {}))

(defn get-sound [pid]
  (if (contains? @person-sound pid)
    (inst-ns (get @person-sound pid))))

(defn add-person-sound! [pid sound]
  (reset! person-sound (assoc @person-sound pid sound))
  ((get-sound pid)))

(defn remove-person-sound! [pid]
  (if (contains? @person-sound pid)
    (reset! person-sound (dissoc @person-sound pid))))

;;;;;;;;;;;;;;;;;;;;;
;; Audio           ;;
;;;;;;;;;;;;;;;;;;;;;

;; TESTING
(definst foo [freq 440] (sin-osc freq))

(def eh (sample "~/work/code/borderless/resources/audio/eh.aif"))
(def eh-buf (load-sample "resources/audio/eh.aif"))

(def noise-1 (sample "~/work/code/borderless/resources/audio/Test21-1.aif"))

;; (demo 10
;;       (rlpf (* 0.5 (saw [338 440]))
;;             (mouse-x 10 10000)
;;             (mouse-y 0.0001 0.9999))) ; cutoff frequency

(definst saw-wave3 [freq 440 attack 5.0 sustain 0.4 release 5.0 vol 0.4 gate 1]
                      (* (env-gen (asr attack sustain release) gate)
                              (saw freq)
                              vol))
;; SYNTHS

(defn tremelo-freq []
  (fn [] (sin-osc:kr 0.5)))

(defn vowel-formant [freq eq-freq q]
  (fn [] (resonz (saw freq) eq-freq q)))

;; (defn vowel-formant2 [freq eq-freq q]
;;   (fn [] (resonz (saw freq) eq-freq q)))


;; (defsynth voice [tremelo-freq:kr 2000]
  ;; (* (env-gen (lin :attack 10 :sustain 5 :release 5) 1 1 0 1 FREE)
  ;;    (+
  ;;     ((vowel-formant tremelo-freq 570 0.1))
  ;;     ((vowel-formant tremelo-freq 840 0.1))
  ;;     ((vowel-formant tremelo-freq 2410 0.1)))))


;; (definst drone-aw [freq 100 verb 0]
;;   "Inst calls the synth macro which takes a synthesizer definition form. The saw function represents a unit-generator, or ugen. These are the basic building blocks for creating synthesizers, and they can generate or process both audio and control signals (odoc saw)"
;;   (let [tremolo-freq (+ freq ((tremelo-freq)))
;;         synth-unit   (* (env-gen (lin :attack 10 :sustain 5 :release 5) 1 1 0 1 FREE)
;;                         (+
;;                          ((vowel-formant tremelo-freq 570 0.1))
;;                          ((vowel-formant tremelo-freq 840 0.1))
;;                          ((vowel-formant tremelo-freq 2410 0.1))))
;;         synth-unit-lpf (rlpf (* 0.5 synth-unit)
;;                                        (mouse-x 10 10000)
;;                                        (mouse-y 0.0001 0.9999))]
;;     (out 0 (free-verb synth-unit-lpf verb verb verb))
;;     (out 1 (free-verb synth-unit-lpf verb verb verb))))


(definst drone-eh [freq 100]
  (let [tremolo-freq (+ freq ((tremelo-freq)))
        synth-unit (* (env-gen (lin :sustain 2) 1 1 0 1 FREE)
                      (+
                       ((vowel-formant tremolo-freq 530 0.1))
                       ((vowel-formant tremolo-freq 1840 0.1))
                       ((vowel-formant tremolo-freq 2480 0.1))))]
    synth-unit))


(definst drone-aw-sus [freq 300 verb 1 kr-mul 25 amp 1
                       attack 5.0 sustain 0.4 release 5.0 gate 1]

  (let [synth-unit          (+
                             ((vowel-formant (+ freq (sin-osc:kr (* 2.5 kr-mul))) 570 0.1))
                             ((vowel-formant (+ freq (sin-osc:kr (* 0.5 kr-mul))) 840 0.1))
                             ((vowel-formant (+ freq (sin-osc:kr (* 1.5 kr-mul))) 2410 0.1)))
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-hpf  (hpf synth-unit-env 900)
        synth-unit-lpf  (* amp (rlpf synth-unit-hpf 600 0.6))]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))


(definst drone-ae-sus [freq 100 verb 1 kr-mul 25 amp 0.75
                       attack 35.0 sustain 0.4 release 15.0 gate 1]

  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (* 2.5 kr-mul))) 270 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (* 0.5 kr-mul))) 2290 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (* 1.5 kr-mul))) 3010 0.1)))
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-hpf  (* amp (hpf synth-unit-env 600))
        synth-unit-lpf  (rlpf synth-unit-hpf 8000 0.6)]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))

(definst drone-eh-sus [freq 80 verb 1 kr-mul 25 amp 1
                       attack 5.0 sustain 0.4 release 15.0 gate 1]
  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (* 2.5 kr-mul))) 530 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (* 0.5 kr-mul))) 1840 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (* 1.5 kr-mul))) 2480 0.1)))
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-lpf  (rlpf synth-unit-env 750 0.9)]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))

(definst drone-oo-sus [freq 120 verb 1 kr-mul 25 amp 1
                       attack 5.0 sustain 0.4 release 15.0 gate 1]

  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (* 2.5 kr-mul))) 300 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (* 0.5 kr-mul))) 870 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (* 1.5 kr-mul))) 2240 0.1)))
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-lpf  (rlpf synth-unit-env 600 0.6)]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))

(defn amplitude-mul [age]
  "This is a quick 'n dirty function that lowers the amplitude over time. It was written to compensate the removal of reverb over time and the increase in the perceived loudness of a sound."
  (cond
    (< age 600) 1
    (< age 750) 0.7
    (< age 900) 0.6
    (< age 1000) 0.5
    (>= age 1000) 0.4
    :else 1))

;;;;;;;;;;;;;;;;;;;;;
;; Control         ;;
;;;;;;;;;;;;;;;;;;;;;

(defn end-sound! [pid]
  (if (contains? @person-sound pid)
    (do
      (ctl (get-sound pid) :gate 0)
      (remove-person-sound! pid))))

(defn reset-atom [current-people]
  (if (seq current-people)
    (do
      (end-sound! (ffirst current-people))
      (reset-atom (rest current-people)))))

(defn start-sound! [pid]
  "Start the sound and add it to the atom. Reset that atom every xx number of people."
  (let [number-of-people (count @person-sound)
        reset-val        50]

  (if (= (rem pid reset-val) 0)
    (reset-atom @person-sound))

  (case number-of-people
    0 (add-person-sound! pid "drone-aw-sus")
    1 (add-person-sound! pid "drone-ae-sus")
    2 (add-person-sound! pid "drone-eh-sus")
    3 (add-person-sound! pid "drone-oo-sus")
    "atom full: four people are tracked")))


(defn control-sound
  [pid val]
  "Here's a fn which will take a val between 0 and 1, map it linearly to a value between 50 and 1000 and send the mapped value as the new frequency of foo:"
  (let [verb-val  (scale-range val 0 1000 1 0.3)
        kr-val    (scale-range val 0 1000 25 0)
        amp-val   (amplitude-mul val)]
    (if (contains? @person-sound pid)
      (ctl (get-sound pid)
           :amp amp-val
           :verb verb-val
           :kr-mul kr-val))))
