(ns borderless.sound
  (:use overtone.live))

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
      ((vowel-formant (+ freq (sin-osc:kr 0.5)) 530 0.1))
      ((vowel-formant (+ freq (sin-osc:kr 0.5)) 1840 0.1))
      ((vowel-formant (+ freq (sin-osc:kr 0.5)) 2480 0.1)))))

(definst foo [freq 440] (sin-osc freq))

(defn control-foo
  [val]
 "Here's a fn which will take a val between 0 and 1, map it linearly to a value between 50 and 1000 and send the mapped value as the new frequency of foo:"
  (let [val (scale-range val 0 3000 300 2000)]
    (println val)
   (ctl foo :freq val)))
