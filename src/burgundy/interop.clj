(ns burgundy.interop
  (:require [burgundy.repl :refer :all])
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

(clojure.lang.RT/loadLibrary "psp")

(def api nil)
(defn bind-api! [binding] (alter-var-root #'api #(identity %2) binding))

;; emulator-specific

(defn shutdown! []
  (PSP/shutdown))

(defn step
  ([ax ay]
   (PSP/nstep 0x0000 ax ay)
   (.onUpdate api)
   (execute-repl-queue)
   )
  ([bitmask]
   ;; TODO: use callbacks instead
   (PSP/nstep bitmask 0.0 0.0)
   (.onUpdate api)
   (execute-repl-queue))
  ([]
   (PSP/step)
   (.onUpdate api)
   (execute-repl-queue)
   ))

(def user-home (File. (System/getProperty "user.home")))

(def phantom-brave-jp
  (File. user-home "game/phantom-brave-jp.iso"))
(def phantom-brave-us
  (File. user-home "game/phantom-brave-us.iso"))

(def save-state-directory
  (File. user-home "build/burgundy/save-states/"))

(defn get-save-name [name]
  (File. save-state-directory (str name ".ppst")))

(defn load-state [name]
  (let [save (get-save-name name)]
    (assert (.exists save))
    (PSP/loadSaveState (.getCanonicalPath save))))

;; phantom brave

(defn my-units [] (.getFriendlyUnits api))

(defn enemy-units [] (.getEnemyUnits api))

(defn island-menu-cursor [] (PSP/getIslandMenuCursorPos))

(defn status-menu-cursor [] (PSP/getStatusMenuCursorPos))

(defn battle-menu-cursor [] (PSP/getBattleMenuCursorPos))

(defn battle-unit-cursor [] (PSP/getBattleUnitMenuCursorPos))

(defn battle-attack-cursor [] (PSP/getBattleAttackMenuCursorPos))

(defn battle-confine-cursor [] (PSP/getConfineMenuCursorPos))

(defn contiguous-memory
  "Returns count wrapped buffers of size bytes starting at offset."
  [offset size count]
  (let [mem (PSP/readRam offset (* count size))
        objs (partition size mem)]
    (->> objs
         (map byte-array)
         (map bytes))))

;; (defn units [] (PSP/getFriendlyUnits))

(def button-masks
  {:square    0x8000
   :triangle  0x1000
   :circle    0x2000
   :cross     0x4000
   :up        0x0010
   :down      0x0040
   :left      0x0080
   :right     0x0020
   :start     0x0008
   :select    0x0001
   :ltrigger  0x0100
   :rtrigger  0x0200})

(def □ [:square])
(def △ [:triangle])
(def ○ [:circle])
(def × [:cross])
(def ↑ [:up])
(def ↓ [:down])
(def ← [:left])
(def → [:right])

(defn wait [frames]
  [[] frames]
  )

(defn button-bits
  "Converts a sequence of button keywords into a button bitmask."
  [buttons]
  (reduce #(bit-or %1 (%2 button-masks)) 0x0000 buttons))

(defn play-input
  "Sends input commands.
  Expects a vector of pairs of a vector of button keywords and the number of frames to hold them for."
  [input]
  (doseq [[buttons frames] input]
    (cond
      (= (first buttons) :analog)
      (dotimes [i frames]
        (step (nth buttons 1) (nth buttons 2)))

      :else (dotimes [i frames]
              (let [bitmask (button-bits buttons)]
                (step bitmask))))))

(defn test-input []
  (play-input [[[:up]    20]
               [[:down]  20]
               [[:left]  20]
               [[:right] 20]
               [[]       40]]))

(defn test-analog []
  (play-input [[[:analog 0.7 1.0] 20]
               [[:analog 0.2 0.0] 20]
               [[:analog -0.1 -1.0] 20]
               [[:analog 1.0 1.0] 20]
               [[]       40]]))

(defn pos-x [unit]
  (.getX unit))

(defn pos-y [unit]
  (.getY unit))

(defn pos-z [unit]
  (.getZ unit))

(defn unit-name [unit]
  (.getName unit))

(defn get-player-pos []
  (let [x (PSP/getPlayerX)
        y (PSP/getPlayerY)
        z (PSP/getPlayerZ)]
    [x y z]))

(defn get-pos [unit]
  (let [x (pos-x unit)
        y (pos-y unit)
        z (pos-z unit)]
    [x y z]))

(defn dist
  ([a b]          (dist (pos-x a) (pos-z a)
                        (pos-x b) (pos-z b)))
  ([unit]         (dist (pos-x unit) (pos-z unit)
                        (PSP/getPlayerX) (PSP/getPlayerZ)))
  ([x1 z1 x2 z2] (Math/sqrt (+ (Math/pow (- x1 x2) 2)
                               (Math/pow (- z1 z2) 2)))))

(defn deg->rad [deg] (* deg (/ Math/PI 180)))
(defn rad->deg [rad] (mod (* rad (/ 180 Math/PI)) 360))

(defn angle-to
  ([unit1 unit2] (angle-to (pos-x unit1) (pos-z unit1)
                           (pos-x unit2) (pos-z unit2)))
  ([unit]        (angle-to (pos-x unit)  (pos-z unit)
                           (PSP/getPlayerX) (PSP/getPlayerZ)))
  ([x1 z1 x2 z2] (rad->deg (Math/atan2 (- z2 z1) (- x2 x1)))))

(defn angle-away
  ([unit1 unit2] (angle-away (pos-x unit1) (pos-z unit1)
                             (pos-x unit2) (pos-z unit2)))
  ([unit]        (angle-away (pos-x unit)  (pos-z unit)
                             (PSP/getPlayerX) (PSP/getPlayerZ)))
  ([x1 z1 x2 z2] (mod (- (angle-to x1 z1 x2 z2) 180) 360)))

(defn point-angle
  "Return [px py] of the pixel location a given angle and distance
  from the unit."
  ([unit angle distance]
   (point-angle (pos-x unit) (pos-z unit) angle distance))
  ([angle distance]
   (point-angle (PSP/getPlayerX) (PSP/getPlayerZ) angle distance))
  ([x z angle distance]
   (let [rad (deg->rad angle)
         px (+ x (* distance (Math/cos rad)))
         pz (+ z (* distance (Math/sin rad)))]
     [px pz]))
  )

(defn angle->analog
  "Return [x y] of the analog inputs for the given angle."
  [angle scale]
  (let [rad (deg->rad angle)
        x (* scale (Math/cos rad))
        y (* scale (Math/sin rad))]
    [x y]))

(defn add-to [angle]
  (let [rad (deg->rad angle)
        radb (deg->rad (+ angle 90))
        x (Math/cos radb)
        y (Math/sin rad)]
    (+ x y)))

(defn closest [unit coll]
  (when (and unit (seq coll))
    (apply min-key (partial dist unit) coll)))

(defn in-range? [unit target dis]
  (< (dist unit target) dis)
  )

(defn dead? [unit]
  (= 0 (.getCurrentHp unit)))

(defn get-closer
  ([unit] (get-closer unit 0.8))
  ([unit within]
   ;; TODO: adjust based on camera angle
   ;; TODO: calculate exact number of frames
   (while (> (dist unit) within)
     (println (dist unit))
     (let [angle (mod (+ (angle-to unit) 225) 360)
           scale (if (> (dist unit) (+ 5.0 within)) 1.0 0.6)
           [ax ay] (angle->analog angle scale)]
       (play-input [[[:analog ax ay] 1]])
       ;; (step)
       ))))
