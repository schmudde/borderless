(ns borderless.sound
  (:use overtone.live)
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utilities            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn control-range
  "I return a function that will return true/false if a value is within a min/max range."
  [min max]
  (fn [value]
    (and (>= value min) (<= value max)) ))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Person/Sound Mapping ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inst-ns
  "This is the instrument name with full namespace qualifiers. Useful becuase the 'sound name' is really the name of the 'definst' macro created by Overtone."
    [instrument]
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

;; VCA, VCO, and FX
(s/def ::vca (s/and number? #((control-range 0.4 1) %) ))
(s/def ::reverb (s/and integer? #((control-range 0 1000) %)))
(s/def ::vco (s/and integer? #((control-range 0 25) %)))

;; envelope
(s/def ::attack (s/and number? #((control-range 0 10) %)))
(s/def ::sustain (s/and number? #((control-range 0 1) %)))
(s/def ::release (s/and number? #((control-range 0 10) %)))
(s/def ::gate (s/and integer? #((control-range 0 1) %)))

(s/def ::sound-params
  (s/keys :req [::vca ::reverb ::vco]
          :opt [::attack ::sustain ::release ::gate]))

(def synth-defaults
  {::vca 1
   ::reverb 1
   ::vco 25
   ::attack 5.0
   ::sustain 0.4
   ::release 5.0
   ::gate 1})

(defn tremelo-freq []
  (fn [] (sin-osc:kr 0.5)))

(s/fdef vowel-formant
        :args (s/cat :freq number? :eq-freq number? :q number?)
        :ret (s/fspec :args (s/cat :saw clojure.test/function? :freq number?)
                      :ret number?))

(defn vowel-formant [freq eq-freq q]
  (fn [] (resonz (saw freq) eq-freq q)))

(defn synth-unit-layered [freq eq-freq q kr-mul]
  (let [[freq-a freq-b freq-c] eq-freq]
    (overtone.sc.ugen-collide/+
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (sin-osc:kr (* 2.5 kr-mul))) freq-a q))
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (sin-osc:kr (* 0.5 kr-mul))) freq-b q))
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (sin-osc:kr (* 1.5 kr-mul))) freq-c q)))))

(definst drone-eh
  [freq 100]
  (let [eq-freq      [530 1840 2480]
        q            0.1
        synth-unit (* (env-gen (lin :sustain 12) 1 1 0 1 FREE)
                      (synth-unit-layered freq eq-freq q 0.5))
        synth-unit-lpf (rlpf (* 0.5 synth-unit)
                                       (mouse-x 10 10000)
                                       (mouse-y 0.0001 0.9999))]

    synth-unit-lpf))

(definst drone-aw-sus [freq 300
                       amp (synth-defaults ::vca)
                       verb (synth-defaults ::reverb)
                       kr-mul (synth-defaults ::vco)
                       attack (synth-defaults ::attack)
                       sustain (synth-defaults ::sustain)
                       release (synth-defaults ::release)
                       gate (synth-defaults ::gate)]
  (let [eq-freq      [570 840 2410]
        q            0.1
        synth-unit (synth-unit-layered freq eq-freq q 0.5)
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-hpf  (hpf synth-unit-env 900)
        synth-unit-lpf  (* amp (rlpf synth-unit-hpf 600 0.6))]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))


(definst drone-ae-sus [freq 100 verb 1 kr-mul 25 amp 0.75
                       attack 35.0 sustain 0.4 release 15.0 gate 1]

  (let [eq-freq      [270 2290 3010]
        q            0.1
        synth-unit (synth-unit-layered freq eq-freq q 0.5)
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-hpf  (* amp (hpf synth-unit-env 600))
        synth-unit-lpf  (rlpf synth-unit-hpf 8000 0.6)]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))

(definst drone-eh-sus [freq 80 verb 1 kr-mul 25 amp 1
                       attack 5.0 sustain 0.4 release 15.0 gate 1]
  (let [eq-freq      [530 1840 2480]
        q            0.1
        synth-unit (synth-unit-layered freq eq-freq q 0.5)
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-lpf  (rlpf synth-unit-env 750 0.9)]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))

(definst drone-oo-sus [freq 120 verb 1 kr-mul 25 amp 1
                       attack 5.0 sustain 0.4 release 15.0 gate 1]

  (let [eq-freq      [300 870 2240]
        q            0.1
        synth-unit (synth-unit-layered freq eq-freq q 0.5)
        synth-unit-env  (* (env-gen (asr attack sustain release) gate) synth-unit)
        synth-unit-lpf  (rlpf synth-unit-env 600 0.6)]

    (out 0 (free-verb synth-unit-lpf verb verb verb))
    (out 1 (free-verb synth-unit-lpf verb verb verb))))

(defn amplitude-mul
  "This is a quick 'n dirty function that lowers the amplitude over time. It was written to compensate the removal of reverb over time and the increase in the perceived loudness of a sound."
  [age]
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

(defn start-sound!
  "Start the sound and add it to the atom. Reset that atom every xx number of people."
  [pid]
  (let [number-of-people (count @person-sound)
        reset-val        40]

    (if (= (rem pid reset-val) 0)
      (do
        (clear)
    (reset-atom @person-sound)))

  (case number-of-people
    0 (add-person-sound! pid "drone-aw-sus")
    1 (add-person-sound! pid "drone-ae-sus")
    2 (add-person-sound! pid "drone-eh-sus")
    3 (add-person-sound! pid "drone-oo-sus")
    "atom full: four people are tracked")))


(defn control-sound
  "Here's a fn which will take a val between 0 and 1, map it linearly to a value between 50 and 1000 and send the mapped value as the new frequency of foo:"
  [pid val]
  (let [verb-val  (scale-range val 0 1000 1 0.3)
        kr-val    (scale-range val 0 1000 25 0)
        amp-val   (amplitude-mul val)]
    (if (contains? @person-sound pid)
      (ctl (get-sound pid)
           :amp amp-val
           :verb verb-val
           :kr-mul kr-val))))
