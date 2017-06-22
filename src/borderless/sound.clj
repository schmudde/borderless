(ns borderless.sound
  (:require [overtone.live :as o]
            [overtone.live :refer :all] ;; TODO: remove this!
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.test]))

;; TODO: I think you'll find that none of the instruments depend on overtone.live. It is simply a convenient way to start up overtone. If you start the external server, then use overtone.core, you'll have all the same capability.

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

(s/def ::vca-range (s/double-in :min 0.4 :max 1.0))
(s/def ::reverb-range (s/int-in 0 1000))
(s/def ::vco-range (s/int-in 0 25))
(s/def ::q-range (s/double-in :min 0.1 :max 1.0))
(s/def ::frequency-range (s/int-in 20 20000))


;; TODO: What about frequency vs. frequency-range, vca vs. vca-range, etc...?

(s/def ::frequency (s/and number? #((control-range 20 20000) %) ))

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

(defn synth-generator []
  [(last (gen/sample (s/gen ::vca-range) 20))
   (last (gen/sample (s/gen ::reverb-range) 20))
   (last (gen/sample (s/gen ::vco-range) 20))])

(defn synth-hacker! [number]
  (let [mucker (synth-generator)]
    (println mucker)
    (o/ctl number :verb 0 :kr-mul (nth mucker 2))))

(s/fdef vowel-formant
        :args (s/cat :freq ::frequency :eq-freq ::frequency :q ::q-range)
        :ret (s/fspec :args integer?
                      :ret clojure.test/function?))

;;;;;;;;;;;;;;;;;;;;;
;; Audio           ;;
;;;;;;;;;;;;;;;;;;;;;

(defn vowel-formant
  "I EQ a narrow Q on a given frequency to shape a saw waveform of a certain pitch."
  [pitch eq-freq q]
  (fn [] (o/resonz (o/saw pitch) eq-freq q)))

(defn synth-unit-layered [freq eq-freq q kr-mul]
  (let [[freq-a freq-b freq-c] eq-freq]
    (overtone.sc.ugen-collide/+
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (o/sin-osc:kr (* 2.5 kr-mul))) freq-a q))
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (o/sin-osc:kr (* 0.5 kr-mul))) freq-b q))
     ((vowel-formant (overtone.sc.ugen-collide/+ freq (o/sin-osc:kr (* 1.5 kr-mul))) freq-c q)))))

(o/definst drone-eh
  "a testing instrument, not used in runtime"
  [freq 100]
  (let [eq-freq      [530 1840 2480]
        q            0.1
        synth-unit (overtone.sc.ugen-collide/* (o/env-gen (o/lin :sustain 12) 1 1 0 1 o/FREE)
                      (synth-unit-layered freq eq-freq q 0.5))
        synth-unit-lpf (o/rlpf (overtone.sc.ugen-collide/* 0.5 synth-unit)
                                       (o/mouse-x 10 10000)
                                       (o/mouse-y 0.0001 0.9999))]
    synth-unit-lpf))

(defn synth-filter-chain
  "I shape a sound with high pass filters, resonante low pass filters, amplifiers, and reverb."
  [synth-unit amp verb gate hpf-rlpf]
  (let [attack (synth-defaults ::attack)
        sustain (synth-defaults ::sustain)
        release (synth-defaults ::release)
        [hpf-freq rlpf-freq rlpf-q] hpf-rlpf]

    (-> synth-unit
        (overtone.sc.ugen-collide/* (o/env-gen (o/asr attack sustain release) gate))
        (o/hpf hpf-freq)
        (o/rlpf rlpf-freq rlpf-q)
        (overtone.sc.ugen-collide/* amp)
        (o/free-verb verb verb verb))))

(o/definst drone-aw-sus
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

(o/definst drone-ae-sus
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

(o/definst drone-eh-sus
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

(o/definst drone-oo-sus
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
    (< age 600) 10
    (< age 750) 1.7
    (< age 900) 1.6
    (< age 1000) 1.5
    (>= age 1000) 1.4
    :else 1))

(defn end-sound! [pid]
  (when (contains? (deref person-sound) pid) (o/ctl (get-sound pid) :gate 0) (remove-person-sound! pid)))

(defn reset-atom [current-people]
  (when (seq current-people) (end-sound! (ffirst current-people)) (reset-atom (rest current-people))))

(defn start-sound!
  "Start the sound and add it to the atom. Reset that atom every xx number of people."
  [pid]
  (let [number-of-people (count @person-sound)
        reset-val        40]

    (when (zero? (rem pid reset-val)) (o/clear) (reset-atom (deref person-sound)))

    (println "Someone Entered")

  (case number-of-people
    0 (add-person-sound! pid "drone-aw-sus")
    1 (add-person-sound! pid "drone-ae-sus")
    2 (add-person-sound! pid "drone-eh-sus")
    3 (add-person-sound! pid "drone-oo-sus")
    "atom full: four people are tracked")))

(defn control-sound
  "I take a val between 0 and 1000+, map the value, and send it to the instrument's controller."
  [pid val]
  (let [verb-val  (o/scale-range val 0 1000 1 0.3)
        kr-val    (o/scale-range val 0 1000 25 0)
        amp-val   (amplitude-mul val)]
    (if (contains? @person-sound pid)
      (o/ctl (get-sound pid)
           :amp amp-val
           :verb verb-val
           :kr-mul kr-val))))
