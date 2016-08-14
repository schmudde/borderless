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

;; SYNTHS

(defn tremelo-freq []
  (fn [] (sin-osc:kr 0.5)))

(defn vowel-formant [freq eq-freq q]
  (fn [] (resonz
   (saw freq)
   eq-freq
   q)))

(definst drone-aw [freq 100 verb 0]
  "Inst calls the synth macro which takes a synthesizer definition form. The saw function represents a unit-generator, or ugen. These are the basic building blocks for creating synthesizers, and they can generate or process both audio and control signals (odoc saw)"
  (let [tremolo-freq (+ freq ((tremelo-freq)))
        synth-unit (* (env-gen (lin :sustain 2) 1 1 0 1 FREE)
                      (+
                       ((vowel-formant tremolo-freq 570 0.1))
                       ((vowel-formant tremolo-freq 840 0.1))
                       ((vowel-formant tremolo-freq 2410 0.1))))]
    (out 0 (free-verb synth-unit verb verb verb))
    (out 1 (free-verb synth-unit verb verb verb))))


(definst drone-eh [freq 100]
  (let [tremolo-freq (+ freq ((tremelo-freq)))
        synth-unit (* (env-gen (lin :sustain 2) 1 1 0 1 FREE)
                      (+
                       ((vowel-formant tremolo-freq 530 0.1))
                       ((vowel-formant tremolo-freq 1840 0.1))
                       ((vowel-formant tremolo-freq 2480 0.1))))]
    synth-unit))


(definst drone-aw-sus [freq 35 verb 1 kr-mul 25]
  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (+ 2.5 kr-mul))) 570 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 0.5 kr-mul))) 840 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 1.5 kr-mul))) 2410 0.1)))]
    (out 0 (free-verb synth-unit verb verb verb))
    (out 1 (free-verb synth-unit verb verb verb))))

(definst drone-eh-sus [freq 35 verb 1 kr-mul 25]
  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (+ 2.5 kr-mul))) 530 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 0.5 kr-mul))) 1840 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 1.5 kr-mul))) 2480 0.1)))]
    (out 0 (free-verb synth-unit verb verb verb))
    (out 1 (free-verb synth-unit verb verb verb))))

(definst drone-oo-sus [freq 120 verb 1 kr-mul 25]
  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (+ 2.5 kr-mul))) 300 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 0.5 kr-mul))) 870 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 1.5 kr-mul))) 2240 0.1)))]
    (out 0 (free-verb synth-unit verb verb verb))
    (out 1 (free-verb synth-unit verb verb verb))))

(definst drone-ae-sus [freq 35 verb 1 kr-mul 25]
  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (+ 2.5 kr-mul))) 660 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 0.5 kr-mul))) 1720 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 1.5 kr-mul))) 2410 0.1)))]
    (out 0 (free-verb synth-unit verb verb verb))
    (out 1 (free-verb synth-unit verb verb verb))))

;;;;;;;;;;;;;;;;;;;;;
;; Control         ;;
;;;;;;;;;;;;;;;;;;;;;

(defn start-sound! [pid]
  (let [number-of-people (count @person-sound)]
    (case number-of-people
      0 (add-person-sound! pid "drone-aw-sus")
      1 (add-person-sound! pid "drone-eh-sus")
      2 (add-person-sound! pid "drone-oo-sus")
      3 (add-person-sound! pid "drone-ae-sus")
      "atom full: four people are tracked")))

(defn end-sound! [pid]
  (if (contains? @person-sound pid)
    (do
      (kill (get-sound pid))
      (remove-person-sound! pid))))

(defn control-sound
  [id val]
 "Here's a fn which will take a val between 0 and 1, map it linearly to a value between 50 and 1000 and send the mapped value as the new frequency of foo:"
  (let [verb-val (scale-range val 0 1000 1 0)
        kr-val (scale-range val 0 1000 25 0)]
    (println val)
   (ctl (get-sound id) :verb verb-val :kr-mul kr-val)))
