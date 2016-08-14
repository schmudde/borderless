(ns borderless.osc-server
  (:use overtone.osc)
  (:require [borderless.sound :as sound]))

;;;;;;;;;;;;;;;;;;;;;
;; OSC Server      ;;
;;;;;;;;;;;;;;;;;;;;;

;; TSPS sends messages each time an Event occurs
;; address:
;; /TSPS/personEntered  OR
;; /TSPS/personUpdated OR
;; /TSPS/personWillLeave

;; 0: pid;
;; 1: oid;
;; 2: age;
;; 3: centroid.x;
;; 4: centroid.y;
;; 5: velocity.x;
;; 6: velocity.y;
;; 7: depth;
;; 8: boundingRect.x;
;; 9: boundingRect.y;
;; 10: boundingRect.width;
;; 11: boundingRect.height;
;; 12: highest.x
;; 13: highest.y
;; 14: haarRect.x;           - will be 0 if hasHaar == false
;; 15: haarRect.y;           - will be 0 if hasHaar == false
;; 16: haarRect.width;       - will be 0 if hasHaar == false
;; 17: haarRect.height;      - will be 0 if hasHaar == false
;; 18: opticalFlowVectorAccumulation.x;
;; 19: opticalFlowVectorAccumulation.y;
;; 20+ : contours (if enabled)

(def PORT 12000)

;(def server (osc-server PORT))
;(def client (osc-client "localhost" PORT))

(defn close-down! []
  ;; remove handler
  (osc-rm-handler server "/TSPS/personEntered")

  ;; stop listening and deallocate resources
  (osc-close server))


(defn person-updated []
  "(osc-handle server '/test' (fn [msg] (println 'MSG: ' msg)))

    Register a handler function for the /test OSC address.
    The handler takes a message map with the following keys:
      [:src-host, :src-port, :path, :type-tag, :args]

    Full Response Example -

    {:path /TSPS/personEntered/,
     :type-tag iiifffffffffffffffff,
     :args (409 0 2 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0), :src-host localhost, :src-port 49551}"

  (osc-handle server "/TSPS/personUpdated"
              (fn [msg]
                (let [ id (nth (:args msg) 0)
                      age (nth (:args msg) 2)]
                  (sound/control-sound id age)))))

(defn person-leave []
    (osc-handle server "/TSPS/personWillLeave"
              (fn [msg]
                (let [ id (nth (:args msg) 0)
                      age (nth (:args msg) 2)]
                  ;; TODO: end-sound call does not work!
                  (sound/end-sound! id)))))

(defn person-enter []
    (osc-handle server "/TSPS/personEntered"
              (fn [msg]
                (let [ id (nth (:args msg) 0)
                      age (nth (:args msg) 2)]
                  (sound/start-sound! id)))))

(defn person []
  (sound/control-sound (nth  '(409 0 20 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0) 2))
)
