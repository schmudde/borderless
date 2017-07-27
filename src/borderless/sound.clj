(ns borderless.sound
  (:require [overtone.core :as o :refer [out hold FREE]]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.test]))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utilities            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn control-range
  "I return a function that will return true/false if a value is within a min/max range."
  [min max]
  (fn [value]
    (and (>= value min) (<= value max)) ))

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
  {::vca 8 ;; TODO: arbitrarily changed this from 1. What is the correct value?
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

(s/def ::pitch ::frequency-range)
(s/def ::eq-freq (s/coll-of ::frequency-range))
(s/def ::hpf-rlpf (s/coll-of number?))
(s/def ::q ::q-range)
(s/def ::drone (s/keys :req [::pitch ::eq-freq ::hpf-rlpf ::q]
                       :opt [::amp ::verb ::mod-rate]))

(s/fdef ctl-names
        :args (s/cat :sound-symbol symbol? :parameter keyword?)
        :ret keyword?)

;;;;;;;;;;;;;;;;;;;;;
;; Audio           ;;
;;;;;;;;;;;;;;;;;;;;;

(defn synth-unit-layered
  "I create a stack of oscilators, each narrowly EQed on a given Q (frequency) to shape a saw waveform of a certain pitch.
   The end results are sharp Q spikes that resemble vowel formants like eh, aw, ae, etc...

   ex: (synth-unit-layered 200 [530 1840 2480] 0.1 25)
   --> (#<sc-ugen: binary-op-u-gen:ar [4]> #<sc-ugen: binary-op-u-gen:ar [4]> #<sc-ugen: binary-op-u-gen:ar [4]>)"

  [freq eq-freq q mod-rate]
  (let [[freq-a freq-b freq-c] eq-freq
        mod-multipliers [2.5 0.5 1.5]
        pitch-with-kr (map #(overtone.sc.ugen-collide/+ freq (o/sin-osc:kr (* % mod-rate))) mod-multipliers)]

    (overtone.sc.ugen-collide/+
        (map #(o/resonz (o/saw %) %2 q) pitch-with-kr eq-freq))))

(defn synth-filter-chain
  "I shape a sound with high pass filters, resonante low pass filters, amplifiers, and reverb."
  [synth-unit amp verb gate hpf-rlpf mod-rate-ctl]
  (let [attack (synth-defaults ::attack)
        sustain (synth-defaults ::sustain)
        release (synth-defaults ::release)
        [hpf-freq rlpf-freq rlpf-q] hpf-rlpf]

    (-> synth-unit
        (overtone.sc.ugen-collide/* (o/env-gen (o/asr attack sustain release) gate))
        (o/hpf hpf-freq)
        (o/rlpf rlpf-freq rlpf-q)
        (overtone.sc.ugen-collide/* amp)
        (overtone.sc.ugen-collide/* mod-rate-ctl)
        (o/free-verb verb verb verb))))

(def drones {::drone-aw {::pitch 300 ::eq-freq [570 840 2410]  ::hpf-rlpf [900 600 0.6] ::q 0.1}
             ::drone-oo {::pitch 120 ::eq-freq [300 870 2240]  ::hpf-rlpf [0 600 0.6] ::q 0.1}
             ::drone-ae {::pitch 100 ::eq-freq [270 2290 3010] ::hpf-rlpf [600 8000 0.6] ::q 0.1}
             ::drone-eh {::pitch 80  ::eq-freq [530 1840 2480] ::hpf-rlpf [0 750 0.9] ::q 0.1}})

(defn ctl-names
  "I take a generated sound's symbol (ex: 'drone-aw11111) and a parameter (ex: :freq) and destructure the generated control values (ex: :freq > freq__20280__auto__).
   I return the generated control value.

   Example: (ctl-names 'drone-aw20496 :amp) => :amp__20282__auto__"

  [sound-symbol parameter]
  (let [ctl-data (o/ctl (eval sound-symbol))
        {params :params} ctl-data
        [freq gate amp] params
        gensym-keymap {:freq (freq :name) :gate (gate :name) :amp (amp :name)}]

    (keyword (gensym-keymap parameter))))

(defmacro sound-returner [drone]
  "I take a sound's name as defined using clojure.spec and call the definst macro to generate an instrument on the fly

   Example: (sound-returner 'drone-eh') => #<instrument: drone-eh24097>

   Then the user can use Overtone's ctl function to update the sound.

   Example: (o/ctl drone-eh24097 (ctl-names 'drone-eh24097 :amp) 10)"

  (let [inst-name (gensym drone)
        synth-drone (drones (keyword "borderless.sound" drone))]
    `(do
       (o/definst ~inst-name [freq#  (~synth-drone ::pitch)
                              gate#  (~synth-defaults ::gate)
                              amp#   (~synth-defaults ::vca)]
         (let [verb#       (~synth-defaults ::reverb)
               mod-rate#   (~synth-defaults ::vco)
               eq-freq#     (~synth-drone ::eq-freq)
               hpf-rlpf#   (~synth-drone ::hpf-rlpf)
               q#          (~synth-drone ::q)
               synth-unit# (synth-unit-layered freq# eq-freq# q# mod-rate#)]

           (synth-filter-chain synth-unit# amp# verb# gate# hpf-rlpf# mod-rate#)
           )))))

(defn sound-maker
  "I make a vowel sound at a given frequency.
   I start/stop with the gate set to 1 or 0."
  [drone]
  (let [synth-drone (drones (keyword "borderless.sound" drone))]
    ((o/synth (o/out 0 (let  [freq   (synth-drone ::pitch)
                              gate   (synth-defaults ::gate)
                              amp    (synth-defaults ::vca)
                              verb   (synth-defaults ::reverb)
                              mod-rate   (synth-defaults ::vco)
                              eq-freq    (synth-drone ::eq-freq)
                              hpf-rlpf   (synth-drone ::hpf-rlpf)
                              q          (synth-drone ::q)
                              synth-unit (synth-unit-layered freq eq-freq q mod-rate)]

                         (synth-filter-chain synth-unit amp verb gate hpf-rlpf mod-rate)))))))

(defn test-frequency
  "This is the most basic use of o/synth"
  []
  (o/synth [] (o/out 0 (o/sin-osc 440))))

(o/definst drone-eh-sus
  "I make the 'eh' vowel sound at a given frequency.
   I start/stop with the gate set to 1 or 0."
  [freq   80
   gate   (synth-defaults ::gate)
   amp    (synth-defaults ::vca)
   verb   (synth-defaults ::reverb)
   mod-rate-ctl 25]

  (let [mod-rate   (synth-defaults ::vco)
        eq-freq    [530 1840 2480]
        hpf-rlpf   [0 750 0.9]
        q          0.1
        synth-unit (synth-unit-layered freq eq-freq q mod-rate)]

    (synth-filter-chain synth-unit amp verb gate hpf-rlpf mod-rate-ctl)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rhythm               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;


(def nome (o/metronome 120))

(defn looper [nome sound]
    (let [beat (nome)]
        (o/at (nome beat) (sound))
        (o/apply-by (nome (inc beat)) looper nome sound [])))

(o/definst trem [freq 440 depth 10 rate 6 length 0.3]
    (* 0.3
       (o/line:kr 0 1 length FREE)
       (o/saw (+ freq (* depth (o/sin-osc:kr rate))))))

;; #<ScheduledJob id: 1, created-at: Wed 06:17:03s, initial-delay: 1032, desc: "Overtone delayed fn", scheduled? true>

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Person/Sound Mapping ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inst-ns-old
  "This is the instrument name with full namespace qualifiers. Useful becuase the 'sound name' is really the name of the 'definst' macro created by Overtone."
    [instrument]
    (eval (symbol "borderless.sound" instrument)))

(defn inst-ns
    [instrument]
    (sound-maker instrument))

(def person-sound (atom {}))

(defn get-sound [pid]
  (if (contains? @person-sound pid)
    (inst-ns (get @person-sound pid))))

(defn add-person-sound! [pid sound]
  (reset! person-sound (assoc @person-sound pid sound))
  (get-sound pid))

(defn remove-person-sound! [pid]
  (if (contains? @person-sound pid)
    (reset! person-sound (dissoc @person-sound pid))))

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
  (when (contains? (deref person-sound) pid) (o/ctl (get-sound pid) :gate 0)
  	(println "Left: " pid)
  	(remove-person-sound! pid)))

(defn reset-atom [current-people]
  (when (seq current-people) (end-sound! (ffirst current-people)) (reset-atom (rest current-people))))

(defn start-sound!
  "Start the sound and add it to the atom. Reset that atom every xx number of people."
  [pid]
  (let [number-of-people (count @person-sound)
        reset-val        40]

    (when (zero? (rem pid reset-val)) (o/clear) (reset-atom (deref person-sound)))

    (println pid " entered")

  (case number-of-people
    0 (add-person-sound! pid "drone-aw")
    1 (add-person-sound! pid "drone-ae")
    2 (add-person-sound! pid "drone-eh")
    3 (add-person-sound! pid "drone-oo")
    "atom full: four people are tracked")))

  ;; (case number-of-people
  ;;   0 (add-person-sound! pid "drone-aw-sus")
  ;;   1 (add-person-sound! pid "drone-ae-sus")
  ;;   2 (add-person-sound! pid "drone-eh-sus")
  ;;   3 (add-person-sound! pid "drone-oo-sus")
  ;;   "atom full: four people are tracked")))

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
           :mod-rate-ctl kr-val))))


;; TODO -
;; Person Entered
;; - Assign them a number
;; - Add them to a list
;; - Create a sound for them
;; - Associate the sound with the number
;; - Play Sound

;; Person Leave
;; - End sound
;; - Remove number (and therefore sound)

;; Person updated
;; - Query ID number in list
;; - Pass information to sound
;;- Update sound

;; Transducers are composable algorithmic transformations. They are independent from the context of their input and output sources and specify only the essence of the transformation in terms of an individual element. Because transducers are decoupled from input or output sources, they can be used in many different processes - collections, streams, channels, observables, etc. Transducers compose directly, without awareness of input or creation of intermediate aggregates.
;; https://clojure.org/guides/spec
;; Generating synthdefs: https://github.com/overtone/overtone/wiki/Comparing-sclang-and-Overtone-synthdefs
;; OSC -> Transducer -> keyword list



;; TODO -
;; Rhythm
;; - Basic metronome for the first person
;; - Second metronome in phase when they're close together, drifting as they move apart
