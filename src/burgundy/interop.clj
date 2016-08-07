(ns burgundy.interop
  (:require [burgundy.repl :refer :all]
            [burgundy.queue :refer :all])
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

(clojure.lang.RT/loadLibrary "psp")

(def api nil)
(defn bind-api! [binding] (alter-var-root #'api #(identity %2) binding))

(def run-repl? true)

;; emulator-specific

(def user-home (File. (System/getProperty "user.home")))

(def phantom-brave-jp
  (File. user-home "game/phantom-brave-jp.iso"))
(def phantom-brave-us
  (File. user-home "game/phantom-brave-us.iso"))

(defn shutdown! []
  (PSP/shutdown))

(defn restart! []
  (PSP/startEmulator (.getCanonicalPath phantom-brave-us)))

(defn do-update []
  (.onUpdate api)
  (when run-repl?
    (execute-repl-queue)))

(defn step
  ([ax ay]
   (PSP/nstep 0x0000 ax ay)
   (do-update))
  ([bitmask]
   ;; TODO: use callbacks instead
   (PSP/nstep bitmask 0.0 0.0)
   (do-update))
  ([]
   (PSP/nstep 0x0 0.0 0.0)
   (do-update)))

(def save-state-directory
  (File. user-home "build/burgundy/save-states/"))

(defn get-save-name [name]
  (File. save-state-directory (str name ".ppst")))

(defn load-state [name]
  (let [save (get-save-name name)]
    (assert (.exists save))
    (PSP/loadSaveState (.getCanonicalPath save))))

(defn save-state [name]
  (let [save (get-save-name name)]
    (PSP/saveSaveState (.getCanonicalPath save))))

;; phantom brave

(defn units [] (.getUnits api))

(defn my-units [] (.getFriendlyUnits api))

(defn enemy-units [] (.getEnemyUnits api))

(defn item-units [] (.getItemUnits api))

(defn get-unit [id] (.getUnit api))

(defn active-unit [] (.getActiveUnit api))

(defn units-under-cursor [] (.getUnitsUnderCursor api))

(defn selected-unit-index [] (.getSelectedUnitIndex api))

(defn selected-unit [] (.getSelectedUnit api))

(defn island-menu-cursor [] (PSP/getIslandMenuCursorPos))

(defn status-menu-cursor [] (PSP/getStatusMenuCursorPos))

(defn battle-menu-cursor [] (PSP/getBattleMenuCursorPos))

(defn battle-unit-cursor [] (PSP/getBattleUnitMenuCursorPos))

(defn battle-attack-cursor [] (PSP/getBattleAttackMenuCursorPos))

(defn battle-confine-cursor [] (PSP/getConfineMenuCursorPos))

(defn contiguous-memory
  "Returns count arrays of size bytes starting at offset."
  [offset size count]
  (let [mem (PSP/readRam offset (* count size))
        objs (partition size mem)]
    (->> objs
         (map byte-array)
         (map bytes))))

(defn unit-memory [n]
  (seq (nth (contiguous-memory 0x01491080 2136 36) n)))

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

(def □ :square)
(def △ :triangle)
(def ○ :circle)
(def × :cross)
(def ↑ :up)
(def ↓ :down)
(def ← :left)
(def → :right)

(def menu-delay 2)

(defn press
  ([button]
   (press button menu-delay))
  ([button delay]
   [[[button] 1          ]
    [[]       delay]]))

(defn wait [frames]
  [[] frames]
  )

(defn button-bits
  "Converts a sequence of button keywords into a button bitmask."
  [buttons]
  (reduce #(bit-or %1 (%2 button-masks)) 0x0000 buttons))

(defn get-name [unit]
  (.getName unit))

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

(defn do-nothing [frames]
  (play-input
   [(wait frames)]))

(defn list-units []
  (.listUnits api))

(defn summoned-units []
  (.summonedUnits api))

(defn print-flags []
  (PSP/printFlags))

(defn pos-x [unit]
  (.getX unit))

(defn pos-y [unit]
  (.getY unit))

(defn pos-z [unit]
  (.getZ unit))

(defn get-id [unit]
  (.getID unit))

(defn get-mana [unit]
  (.getMana unit))

(defn get-remaining-move [unit]
  (.getRemainingMove unit))

(defn can-move? [unit]
  (> (get-remaining-move unit) 0))

(defn has-attacked?
  ([] (has-attacked? (active-unit)))
  ([unit]
   (.hasAttacked unit)))

(defn is-being-held? [unit]
  (.isBeingHeld unit))

(defn dump [unit]
  (.dump unit))

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

(defn is-marona? [unit]
  (= (get-name unit) "Marona"))

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

(defn closest
  ([coll] (closest (active-unit) coll))
  ([unit coll]
               (when (and unit (seq coll))
                 (apply min-key (partial dist unit) coll))))

(defn in-range?
  ([target-unit range] (in-range? (active-unit) target-unit range))
  ([unit target-unit range]
   (<= (dist unit target-unit) range)))

(defn units-nearby [unit range coll]
  (let [nearby? (fn [target-unit] (in-range? unit target-unit range))
        nearby-units (remove #{unit} (filter nearby? coll))]
    (set nearby-units)))

(def confine-radius 70)
(def confine-upper 1000)
(def confine-lower 1000)

(defn confine-targets []
  (let [nearby-items (units-nearby (active-unit) confine-radius (item-units))]
    (set (remove is-being-held? nearby-items))))

(defn too-close?
  ([target] (too-close? (active-unit) target))
  ([unit target]
   (< (dist unit target) 2.0)))

(defn is-active?
  "Returns true if the cursor can be moved on the map.

  This means it is the player's turn and there are no animations/menus."
  [] (PSP/canMoveInMap))

(defn can-move?
  "Checks if the unit that is trying to move can move to the position
   the cursor is at."
  [] (PSP/canMove))

(defn can-attack?
  "Checks if the unit that was just targeted in the targeting mode
   can be attacked (cursor is not crossed out)."
  [] (PSP/canAttack))

(defn can-confine?
  "Checks if the unit that's being targeted can be confined to."
  [] (PSP/canConfine))

(defn stage-started?
  "Checks if a stage has started."
  [] (PSP/hasStageStarted))

(defn stage-clear?
  "Checks if the result screen after clearing a stage is currently active."
  [] (PSP/isStageClear))

(defn at-special-stage?
  "Checks if the special map screen is active."
  [] (PSP/isSpecialStageScreenUp))

(defn dead? [unit]
  (= 0 (.getCurrentHp unit)))

(def selection-dist 2.0)

(defn move-to
  ([unit] (move-to unit selection-dist))
  ([unit within & [dir]]
   ;; TODO: adjust based on camera angle
   ;; TODO: calculate exact number of frames
   (let [comparator (if (= dir :away) < >)
         angle-fn (if (= dir :away) angle-away angle-to)]
     (while (comparator (dist unit) within)
       ;; (println (dist unit))
       (let [angle (mod (+ (angle-fn unit) 225) 360)
             scale (if (comparator (dist unit) (+ 5.0 within)) 1.0 0.75)
             [ax ay] (angle->analog angle scale)]
         ;; (println [ax ay])
         (play-input [[[:analog ax ay] 1]])
         ;; (step)
         )))))

(defn select-unit-in-cursor
  "When there are multiple units near the cursor, cycles to the given one."
  [unit]
  (let [cursor-units (units-under-cursor)
        current (selected-unit-index)
        ;; by id, not exact copy
        ;; TODO: should this be the default?
        target (.indexOf (map get-id cursor-units) (get-id unit))
        presses
        (cond
          (= target -1)      0
          (> current target) (+ target current)
          :else              (- target current))]
    (println (map get-name cursor-units))
    (println (str current " " target " " presses))
    (play-input
     (apply concat
            (concat
             (repeat presses (press :select 10)))))))

(defn select-unit
  "Moves the cursor to and selects the given unit."
  [unit]
  (move-to unit)
  (select-unit-in-cursor unit))
