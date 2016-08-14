(ns borderless.sound
  (:use overtone.live))

;; <justin_smith> ,(assoc {} 1 :a)
;; <clojurebot> {1 :a}
;; <justin_smith> ,(get {1 :a} 1)
;; <clojurebot> :a
;; <justin_smith> dschmudde: yeah, you usually don't even need to use the
;;                hash-map function itself usually
;; *** bengillies (~bengillie@bengillies.net) has quit: Ping timeout: 244 seconds
;; <justin_smith> ,(assoc {} :a 0 :a 1 :a 3) ; "repeated usages of assoc"
;; <clojurebot> {:a 3}


(def person-sound (atom (assoc {} 1 :drone-eh)))

(defn get-sound [pid]
  (if (contains? @person-sound pid)
    (eval (symbol "borderless.sound" (name (get @person-sound pid))))))

(defn add-person-sound! [pid]
  (reset! person-sound (assoc @person-sound pid :drone-eh-sus)))

(defn remove-person-sound! [pid]
  (if (contains? @person-sound pid)
    (reset! person-sound (dissoc @person-sound pid))))

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
  "Inst calls the synth macro which takes a synthesizer definition form. The saw function represents a unit-generator, or ugen. These are the basic building blocks for creating synthesizers, and they can generate or process both audio and control signals (odoc saw)"
  (* (env-gen (lin :sustain 2) 1 1 0 1 FREE)
     (+
      ((vowel-formant (+ freq (sin-osc:kr 2.5)) 530 0.1))
      ((vowel-formant (+ freq (sin-osc:kr 0.5)) 1840 0.1))
      ((vowel-formant (+ freq (sin-osc:kr 1.5)) 2480 0.1)))))

(definst drone-eh-sus [freq 35 verb 1 kr-mul 25]
  "Inst calls the synth macro which takes a synthesizer definition form. The saw function represents a unit-generator, or ugen. These are the basic building blocks for creating synthesizers, and they can generate or process both audio and control signals (odoc saw)"
  (let [synth-unit  (+
                     ((vowel-formant (+ freq (sin-osc:kr (+ 2.5 kr-mul))) 530 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 0.5 kr-mul))) 1840 0.1))
                     ((vowel-formant (+ freq (sin-osc:kr (+ 1.5 kr-mul))) 2480 0.1)))]
    (out 0 (free-verb synth-unit verb verb verb))
    (out 1 (free-verb synth-unit verb verb verb))))

(definst foo [freq 440] (sin-osc freq))

(defn start-sound! [pid]
  (add-person-sound! pid)
  ((get-sound pid)))

(defn end-sound! [pid]
  (if (contains? @person-sound pid)
    (kill (get-sound pid))
    (remove-person-sound! pid)))

(defn control-sound
  [val]
 "Here's a fn which will take a val between 0 and 1, map it linearly to a value between 50 and 1000 and send the mapped value as the new frequency of foo:"
  (let [verb-val (scale-range val 0 1000 1 0)
        kr-val (scale-range val 0 1000 25 0)]
    (println val)
   (ctl drone-eh-sus :verb verb-val :kr-mul kr-val)))
