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

(defn inst-name-getter
  [inst]
  (-> inst meta :name))

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
  {::vca 1 ;; TODO: arbitrarily changed this from 1. What is the correct value?
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

;; (= :overtone.studio.inst/instrument (type (get @person-sound 1)))
;; (s/fdef ctl-names
;;         :args (s/cat :instrument ??? :parameter keyword?)
;;         :ret keyword?)

;;;;;;;;;;;;;;;;;;;;;
;; Audio Sculpting ;;
;;;;;;;;;;;;;;;;;;;;;

(def drones {::drone-aw {::pitch 300 ::eq-freq [570 840 2410]  ::hpf-rlpf [900 600 0.6] ::q 0.1}
             ::drone-oo {::pitch 120 ::eq-freq [300 870 2240]  ::hpf-rlpf [0 600 0.6] ::q 0.1}
             ::drone-ae {::pitch 100 ::eq-freq [270 2290 3010] ::hpf-rlpf [600 8000 0.6] ::q 0.1}
             ::drone-eh {::pitch 80  ::eq-freq [530 1840 2480] ::hpf-rlpf [0 750 0.9] ::q 0.1}})

(defn synth-unit-layered
  "I create a stack of oscilators, each narrowly EQed on a given Q (frequency) to shape a saw waveform of a certain pitch.
   The end results are sharp Q spikes that resemble vowel formants like eh, aw, ae, etc...

   ex: (synth-unit-layered 200 [530 1840 2480] 0.1 25)
   --> (#<sc-ugen: binary-op-u-gen:ar [4]> #<sc-ugen: binary-op-u-gen:ar [4]> #<sc-ugen: binary-op-u-gen:ar [4]>)"

  [freq eq-freq q mod-rate]
  (let [[freq-a freq-b freq-c] eq-freq
        mod-multipliers [2.5 0.5 1.5]
        pitch-with-kr (map #(overtone.sc.ugen-collide/+ freq (o/sin-osc:kr (overtone.sc.ugen-collide/* % mod-rate))) mod-multipliers)]

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

(defn ctl-names
  "I take an instrument (type - :overtone.studio.inst/instrument) and a keyword parameter (ex: :freq)
   ane destructure the generated control values (ex: :freq > freq__20280__auto__).

   I return the generated control value.

   Example: (ctl-names (get @person-sound 1) :amp) => :amp__20282__auto__"

  [instrument parameter]

  (if-let [instrument-symbol (inst-name-getter instrument)]
    ;; If the instrument doesn't exist, it will return nil and not execute the rest of the closure.

    (let [ctl-data (o/ctl (eval instrument-symbol)) ;; Grab the control data map
          {params :params} ctl-data                 ;; Grab the parametrs from the control data
          [freq gate amp verb mod-rate] params      ;; Destructure the parameters
          gensym-keymap {:freq (freq :name)
                         :gate (gate :name)
                         :amp (amp :name)
                         :verb (verb :name)
                         :mod-rate (mod-rate :name)}]

      (keyword (gensym-keymap parameter)))))

(defmacro sound-returner [drone]
  "I take a sound's name as defined using clojure.spec and call the definst macro to generate an instrument on the fly

   Example: (sound-returner 'drone-eh') => #<instrument: drone-eh24097>

   Then the user can use Overtone's ctl function to update the sound.

   Example: (o/ctl drone-eh24097 (ctl-names 'drone-eh24097 :amp) 10)"

  (let [inst-name (gensym drone)
        synth-drone (drones (keyword "borderless.sound" drone))]
    `(do
       (o/definst ~inst-name [freq#     (~synth-drone ::pitch)
                              gate#     (~synth-defaults ::gate)
                              amp#      (~synth-defaults ::vca)
                              verb#     (~synth-defaults ::reverb)
                              mod-rate# (~synth-defaults ::vco)]
         (let [eq-freq#    (~synth-drone ::eq-freq)
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

(def person-sound (atom {}))

(defn get-sound [pid]
  (if (contains? @person-sound pid)
    ((get @person-sound pid))))

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
  (let [instrument (get @person-sound pid)
        instrument-symbol (inst-name-getter instrument)
        gate-name (ctl-names instrument :gate)]
    (when (contains? (deref person-sound) pid) (o/ctl (eval instrument-symbol) gate-name 0)
          (println "Left: " pid)
          (remove-person-sound! pid))))

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
    0 (add-person-sound! pid (sound-returner "drone-aw"))
    1 (add-person-sound! pid (sound-returner "drone-oo"))
    2 (add-person-sound! pid (sound-returner "drone-ae"))
    3 (add-person-sound! pid (sound-returner "drone-eh"))
    "atom full: four people are tracked")))

(defn control-sound
  "I take a val between 0 and 1000+, map the value, and send it to the instrument's controller."
  [pid val]
  (let [instrument (get @person-sound pid)
        instrument-symbol (inst-name-getter instrument)

        amp-name (ctl-names instrument :amp)
        verb-name (ctl-names instrument :verb)
        mod-name (ctl-names instrument :mod-rate)

        amp-val   (amplitude-mul val)
        verb-val  (o/scale-range val 0 1000 1 0.3)
        kr-val    (o/scale-range val 0 1000 25 4)]
    (if (contains? @person-sound pid)
      (o/ctl (eval instrument-symbol)
           amp-name amp-val
           verb-name verb-val
           mod-name kr-val))))


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

;; {:name "drone-aw23914", :params (
;;                                  {:name "freq__23702__auto__", :default 300.0, :rate :kr, :value #atom[300.0 0x7ebe010a]}
;;                                  {:name "gate__23703__auto__", :default 1.0, :rate :kr, :value #atom[1.0 0x2f278a21]}
;;                                  {:name "amp__23704__auto__", :default 8.0, :rate :kr, :value #atom[8.0 0x36cfe335]}),
;;  :args ("freq__23702__auto__" "gate__23703__auto__" "amp__23704__auto__"),
;;  :sdef {:name "borderless.sound/drone-aw23914", :constants [0.0 0 1.0 900.0 840.0 5.0 53.0 37.5 600.0 12.5 25.0 62.5 570.0 2410.0 -4 0.4 1 0.1 2 5 0.6 -99], :params (300.0 1.0 8.0), :pnames ({:name "freq__23702__auto__", :index 0} {:name "gate__23703__auto__", :index 1} {:name "amp__23704__auto__", :index 2}), :ugens ({:args nil, :special 0, :name "Control", :rate 1, :inputs (), :rate-name :kr, :n-outputs 3, :id 1365, :outputs ({:rate 1} {:rate 1} {:rate 1}), :n-inputs 0} #<sc-ugen: sin-osc:kr [0]> #<sc-ugen: binary-op-u-gen:kr [2]> #<sc-ugen: saw:ar [3]> #<sc-ugen: resonz:ar [4]> #<sc-ugen: binary-op-u-gen:ar [5]> #<sc-ugen: env-gen:kr [1]> #<sc-ugen: binary-op-u-gen:ar [8]> #<sc-ugen: hpf:ar [9]> #<sc-ugen: rlpf:ar [10]> #<sc-ugen: binary-op-u-gen:ar [12]> #<sc-ugen: sin-osc:kr [0]> #<sc-ugen: binary-op-u-gen:kr [2]> #<sc-ugen: saw:ar [3]> #<sc-ugen: resonz:ar [4]> #<sc-ugen: binary-op-u-gen:ar [5]> #<sc-ugen: binary-op-u-gen:ar [8]> #<sc-ugen: hpf:ar [9]> #<sc-ugen: rlpf:ar [10]> #<sc-ugen: sin-osc:kr [0]> #<sc-ugen: binary-op-u-gen:kr [2]> #<sc-ugen: saw:ar [3]> #<sc-ugen: resonz:ar [4]> #<sc-ugen: binary-op-u-gen:ar [12]> #<sc-ugen: binary-op-u-gen:ar [13]> #<sc-ugen: binary-op-u-gen:ar [5]> #<sc-ugen: binary-op-u-gen:ar [8]> #<sc-ugen: hpf:ar [9]> #<sc-ugen: rlpf:ar [10]> #<sc-ugen: binary-op-u-gen:ar [12]> #<sc-ugen: binary-op-u-gen:ar [13]> #<sc-ugen: free-verb:ar [14]> #<sc-ugen: free-verb:ar [14]> #<sc-ugen: binary-op-u-gen:ar [13]> #<sc-ugen: free-verb:ar [14]> #<sc-ugen: out:ar [45]>)},
;;  :group #<synth-group [live] : Inst drone-aw23914 Container 102>,
;;  :instance-group #<synth-group [live] : Inst drone-aw23914 103>,
;;  :fx-group #<synth-group [live] : Inst drone-aw23914 FX 104>, :mixer #<synth-node [live] : overtone.s547/stereo-inst-mixer 105>,
;;  :bus #<audio-bus: No Name, 3 channels, id 53>, :fx-chain [], :volume #atom[1.0 0x6aa35da8], :pan #atom[0.0 0x641ba7a7], :n-chans 3}
