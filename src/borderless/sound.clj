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
;; spec            ;;
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

(s/fdef vowel-formant
        :args (s/cat :freq number? :eq-freq number? :q number?)
        :ret (s/fspec :args (s/cat :saw clojure.test/function? :freq number?)
                      :ret number?))

;;;;;;;;;;;;;;;;;;;;;
;; Audio           ;;
;;;;;;;;;;;;;;;;;;;;;

(defn vowel-formant
  "I EQ a narrow Q on a given frequency to shape a saw waveform of a certain pitch."
  [pitch eq-freq q]
  (fn [] (resonz (saw pitch) eq-freq q)))

(defn synth-unit-layered [freq eq-freq q kr-mul]
  (let [[freq-a freq-b freq-c] eq-freq]
    (overtone.sc.ugen-collide/+
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (sin-osc:kr (* 2.5 kr-mul))) freq-a q))
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (sin-osc:kr (* 0.5 kr-mul))) freq-b q))
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (sin-osc:kr (* 1.5 kr-mul))) freq-c q)))))

(definst drone-eh
  "a testing instrument, not used in runtime"
  [freq 100]
  (let [eq-freq      [530 1840 2480]
        q            0.1
        synth-unit (* (env-gen (lin :sustain 12) 1 1 0 1 FREE)
                      (synth-unit-layered freq eq-freq q 0.5))
        synth-unit-lpf (rlpf (* 0.5 synth-unit)
                                       (mouse-x 10 10000)
                                       (mouse-y 0.0001 0.9999))]
    synth-unit-lpf))

(defn synth-filter-chain
  "I shape a sound with high pass filters, resonante low pass filters, amplifiers, and reverb."
  [synth-unit amp verb gate hpf-rlpf]
  (let [attack (synth-defaults ::attack)
        sustain (synth-defaults ::sustain)
        release (synth-defaults ::release)
        [hpf-freq rlpf-freq rlpf-q] hpf-rlpf]

    (-> synth-unit
        (overtone.sc.ugen-collide/* (env-gen (asr attack sustain release) gate))
        (hpf hpf-freq)
        (rlpf rlpf-freq rlpf-q)
        (overtone.sc.ugen-collide/* amp)
        (free-verb verb verb verb))))

(definst drone-aw-sus
  "I make the 'aw' vowel sound at a given frequency.
   I start/stop with the gate set to 1 or 0."
  [freq   300
   gate   (synth-defaults ::gate)
   amp    (synth-defaults ::vca)
   verb   (synth-defaults ::reverb)
   kr-mul (synth-defaults ::vco)]

    (let [kr-mul     (:value kr-mul)
          eq-freq    [570 840 2410]
          hpf-rlpf   [900 600 0.6]
          q          0.1
          synth-unit (synth-unit-layered freq eq-freq q kr-mul)]

        (synth-filter-chain synth-unit amp verb gate hpf-rlpf)))

(definst drone-ae-sus
  "I make the 'ae' vowel sound at a given frequency.
   I start/stop with the gate set to 1 or 0."
  [freq   100
   gate   (synth-defaults ::gate)
   amp    (synth-defaults ::vca)
   verb   (synth-defaults ::reverb)
   kr-mul (synth-defaults ::vco)]

  (let [kr-mul     (:value kr-mul)
        eq-freq    [270 2290 3010]
        hpf-rlpf   [600 8000 0.6]
        q          0.1
        synth-unit (synth-unit-layered freq eq-freq q kr-mul)]

    (synth-filter-chain synth-unit amp verb gate hpf-rlpf)))

(definst drone-eh-sus
  "I make the 'eh' vowel sound at a given frequency.
   I start/stop with the gate set to 1 or 0."
  [freq   80
   gate   (synth-defaults ::gate)
   amp    (synth-defaults ::vca)
   verb   (synth-defaults ::reverb)
   kr-mul (synth-defaults ::vco)]

  (let [kr-mul     (:value kr-mul)
        eq-freq    [530 1840 2480]
        hpf-rlpf   [0 750 0.9]
        q          0.1
        synth-unit (synth-unit-layered freq eq-freq q kr-mul)]

    (synth-filter-chain synth-unit amp verb gate hpf-rlpf)))

(definst drone-oo-sus
  "I make the 'oo' vowel sound at a given frequency.
   I start/stop with the gate set to 1 or 0."
  [freq   120
   gate   (synth-defaults ::gate)
   amp    (synth-defaults ::vca)
   verb   (synth-defaults ::reverb)
   kr-mul (synth-defaults ::vco)]

  (let [kr-mul     (:value kr-mul)
        eq-freq    [300 870 2240]
        hpf-rlpf   [0 600 0.6]
        q          0.1
        synth-unit (synth-unit-layered freq eq-freq q kr-mul)]

    (synth-filter-chain synth-unit amp verb gate hpf-rlpf)))

;;;;;;;;;;;;;;;;;;;;;
;; Control         ;;
;;;;;;;;;;;;;;;;;;;;;

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
  "I take a val between 0 and 1000+, map the value, and send it to the instrument's controller."
  [pid val]
  (let [verb-val  (scale-range val 0 1000 1 0.3)
        kr-val    (scale-range val 0 1000 25 0)
        amp-val   (amplitude-mul val)]
    (if (contains? @person-sound pid)
      (ctl (get-sound pid)
           :amp amp-val
           :verb verb-val
           :kr-mul kr-val))))
