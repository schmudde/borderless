(ns borderless.osc-server
  (:require [overtone.osc :as osc]
            [borderless.sound :as sound]))

;;;;;;;;;;;;;;;;;;;;;
;; OSC Server      ;;
;;;;;;;;;;;;;;;;;;;;;

;; https://github.com/overtone/overtone
;; https://github.com/overtone/osc-clj

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test Message:
;;
;; (defn osc-send
;;   [client path & args]
;; (osc-send-msg client (apply mk-osc-msg path (osc-type-tag args) args)))
;;
;; THIS WORKS: (osc/osc-send client "/TSPS/personEntered" 4 0 100 0.1)
;;             (osc/osc-send client "/TSPS/personUpdated" 4 0 800 0.1) <-- 4 is :arg 0, 0 is :arg 1, 800 is :arg 2, 0.1 is :arg 3
;;             (osc/osc-send client "/TSPS/personWillLeave" 4 0 100 0.1)


(def PORT 12000)

(def server (osc/osc-server PORT))
(def client (osc/osc-client "localhost" PORT))


(defn person-updated
  " (osc-handle server '/test' (fn [msg] (println 'MSG: ' msg)))

    Register a handler function for the /test OSC address.
    The handler takes a message map with the following keys:
      [:src-host, :src-port, :path, :type-tag, :args]

    Full Response Example -

    {:path /TSPS/personEntered/,
     :type-tag iiifffffffffffffffff,
     :args (409 0 2 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0), :src-host localhost, :src-port 49551}"
  []
  (osc/osc-handle server "/TSPS/personUpdated"
              (fn [msg]
                (let [[id oid age centeroid-x] (msg :args)]
                  (sound/controller :timbre id age)
                  (sound/controller :rate id centeroid-x)))))

(defn person-leave []
    (osc/osc-handle server "/TSPS/personWillLeave"
              (fn [msg]
                (let [ id (nth (:args msg) 0)]
                  (sound/end-sound! id)
                  (println "Someone left. these people are still here: " (deref borderless.sound/person-sound))))))

(defn person-enter
  []
    (osc/osc-handle server "/TSPS/personEntered"
              (fn [msg]
                (let [[id oid age centeroid-x] (msg :args)]
                  (sound/start-sound! id centeroid-x)
                  (println "Someone Came in! Full list: " (deref borderless.sound/person-sound))))))

(defn person []
  (sound/controller :timbre (nth  '(409 0 20 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0) 2) 1)
)

(def test-msg {:path "/TSPS/personEntered/",
               :type-tag "iiifffffffffffffffff",
               :args '(409 0 2 0.07673444 0.86617285 0.0 0.0 0.0 0.015625 0.73125 0.128125 0.25625 -0.0015625 0.0020833334 0.0 0.0 0.0 0.0 0.0 0.0), :src-host "localhost", :src-port 49551})

(defn open-server! []
  ;; create handlers
  (person-updated)
  (person-leave)
  (person-enter))

(defn close-down! []
  ;; remove handler
  (osc/osc-rm-handler server "/TSPS/personEntered")

  ;; stop listening and deallocate resources
  (osc/osc-close server))

;;;;;;;;;;;;;;;;;;;;;
;; Start Server    ;;
;;;;;;;;;;;;;;;;;;;;;

(open-server!)
