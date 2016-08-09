(ns burgundy.interop
  (:require [burgundy.repl :refer :all]
            [burgundy.queue :refer :all]
            [clojure.pprint :refer (cl-format)]
            [clojure.string :as string]
            [clojure.data :refer [diff]]
            [wharf.core :refer :all])
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

(declare dist)

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

(defmacro doseq-indexed [index-sym [item-sym coll] & body]
  `(doseq [[~index-sym ~item-sym] (map list (range) ~coll)]
     ~@body))

;; phantom brave

(defn units [] (.getUnits api))

(defn my-units [] (.getFriendlyUnits api))

(defn enemy-units [] (.getEnemyUnits api))

(defn item-units [] (.getItemUnits api))

(defn get-unit [id] (.getUnit api id))

(defn active-unit [] (.getActiveUnit api))

(defn units-under-cursor [] (.getUnitsUnderCursor api))

(defn selected-unit-index [] (.getSelectedUnitIndex api))

(defn selected-unit [] (.getSelectedUnit api))

(defn skill-types [] (.getSkillTypes api))

(defn get-skill-type [id] (.getSkillType api id))

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
  [[[] frames]]
  )

(defn button-bits
  "Converts a sequence of button keywords into a button bitmask."
  [buttons]
  (reduce #(bit-or %1 (%2 button-masks)) 0x0000 buttons))

(defn space->hyphen [s]
  (string/join "-" (string/split s #" ")))

(defn strip [coll chars]
  (apply str (remove #((set chars) %) coll)))

(defn strip-bad-chars [s]
  (strip s "/@!'.,"))

(defn camel->keyword []
  (comp keyword string/lower-case space->hyphen strip-bad-chars))

(defmacro gen-type-kw-map [inject-sym coll]
  `(def ~inject-sym
     (zipmap (map (comp (camel->keyword) #(.getName %)) (seq ~coll))
             (map #(.getID %) (seq ~coll)))))

(defn gen-type-kw-maps []
  (gen-type-kw-map skill-type-kws (skill-types)))

(defmacro defn-unit
  "Define a function that either takes a unit argument, or takes no arguments and is applied to the active unit."
  [name args body]
  (let [other (rest args)]
    `(defn ~name
       ([~@other] (~name (active-unit) ~@other))
       (~args ~body))))

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
   (wait frames)))

(defn list-units []
  (.listUnits api))

(defn summoned-units []
  (.summonedUnits api))

(defn print-flags []
  (PSP/printFlags))

(defn-unit pos-x [unit]
  (.getX unit))

(defn-unit pos-y [unit]
  (.getY unit))

(defn-unit pos-z [unit]
  (.getZ unit))

(defn-unit vel-x [unit]
  (.getVelX unit))

(defn-unit vel-y [unit]
  (.getVelY unit))

(defn-unit vel-z [unit]
  (.getVelZ unit))

(defn-unit get-pos [unit]
  (let [x (pos-x unit)
        y (pos-y unit)
        z (pos-z unit)]
    [x y z]))

(defn get-player-pos []
  (let [x (PSP/getPlayerX)
        y (PSP/getPlayerY)
        z (PSP/getPlayerZ)]
    [x y z]))

(defn-unit is-in-air? [unit]
  (> (Math/abs (vel-y unit)) 1.0))

(defn-unit is-moving? [unit]
  (or (> (Math/abs (vel-x unit 1.0)))
      (> (Math/abs (vel-y unit 1.0)))
      (> (Math/abs (vel-z unit 1.0)))))

(defn-unit get-name [unit]
  (.getName unit))

(defn-unit get-id [unit]
  (.getID unit))

(defn-unit get-mana [unit]
  (.getMana unit))

(defn-unit get-max-move [unit]
  (.getRemainingMove unit))

(defn-unit get-remaining-move [unit]
  (.getRemainingMove unit))

(defn-unit get-held-unit [unit]
  (get-unit (.getHeldItemID unit)))

(def skill-sp-ids
  {:physical 0
   :energy 1
   :elemental 2
   :natural 3
   :spacetime 4
   :alteration 5
   :healing 6})

(def skill-sp-kws
  (clojure.set/map-invert skill-sp-ids))

(def skill-attack-types
  {4 :recovery
   5 :support
   7 :combo
   12 :atk
   13 :def
   14 :int
   15 :res
   16 :spd})

(defn-unit get-skills [unit]
  (.getSkills unit))

(defn get-skill-type [skill-or-id]
  (.getSkillType api
                 (if (number? skill-or-id)
                   skill-or-id
                   (.getID skill-or-id))))

(defn skill-name [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getName skill)))

(defn skill-sp-cost [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getSpCost skill)))

(defn skill-sp-type [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (get skill-sp-kws (.getSpType skill))))

(defn print-skill [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (println (.getName skill)
             (.getDesc skill)
             (.getSpCost skill))))

(defn-unit get-all-skills [unit]
  (concat (get-skills unit)
          (get-skills (get-held-unit unit))))

(defn-unit get-sp [unit]
  (do (println unit) (zipmap (vals skill-sp-kws) (.getSp unit))))

(defn-unit get-max-sp [unit]
  (zipmap (vals skill-sp-kws) (.getMaxSp unit)))

(defn-unit get-sp-affinity [unit]
  (zipmap (vals skill-sp-kws) (.getSpAffinity unit)))

(defn can-use-skill?
  "Given a map of sp values and a skill, determines if the skill can be used.
  It takes SP instead of a unit because the skill owner might be an item, not the user."
  [all-sp skill-or-id]
  (do (let [sp-type (skill-sp-type skill-or-id)
            sp (sp-type all-sp)]
        (> sp (skill-sp-cost skill-or-id)))))

(defn-unit usable-skills [unit]
  (let [skills (get-all-skills unit)
        sp     (get-sp unit)]
    (filter (partial can-use-skill? sp) skills)))

(defn has-moved? []
  (not= (get-max-move (active-unit)) (get-remaining-move (active-unit))))

(def no-move-threshold 5.0)

(defn has-move-remaining? []
  (> (get-remaining-move (active-unit)) no-move-threshold))

(defn-unit has-attacked? [unit]
  (.hasAttacked unit))

(defn-unit is-being-held? [unit]
  (.isBeingHeld unit))

(defn-unit is-friendly? [unit]
  (.isFriendly unit))

(defn-unit is-marona? [unit]
  (= (get-name unit) "Marona"))

(defn-unit dump [unit]
  (.dump unit))

(def unit-start-offset 0x01491090)
(def unit-size 2136)

(defn-unit unit-memory [unit]
  (seq (nth (contiguous-memory unit-start-offset unit-size 36) (get-id unit))))

(defn-unit unit-offset [unit]
  (+ unit-start-offset (* (get-id unit) unit-size)))

(defn-unit unit-byte [unit n]
  (nth (unit-memory unit) n))

(defn diff-memory
  "Given two pieces of data, returns an array with nil values where the same data was found in the array."
  [a b]
  (map (fn [i j]
         (if (and (not= i j) i j)
           j
           nil))
       a b))

(defn diff-all [seq]
  (reduce diff-memory seq))

(defn print-diff [d]
  (doseq-indexed i [b d]
                 (when-not (nil? b)
                   (printf "0x%04X: %02X \n" i b))))

(defn to-bits [i]
  (str "2r" (Integer/toBinaryString i)))

(def skill-shapes
  {0 :sphere
   1 :column
   2 :triangle})

;; TODO: temporary
(defn is-spherical? [skill-or-id]
  (= (:sphere skill-shapes)
     (.getShape (get-skill-type skill-or-id))))

(defn get-skill-shape [skill-type]
  (get skill-shapes (.getShape skill-type)))

(defn get-skill-range [skill-type]
  (.getRange skill-type))

(defn get-skill-radius [skill-type]
  (.getRadius skill-type))

(defn get-skill-limit-upper [skill-type]
  (.getLimitUpper skill-type))

(defn get-skill-limit-lower [skill-type]
  (.getLimitLower skill-type))

(defn get-skill-info [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    {:shape  (get-skill-shape skill)
     :range  (get-skill-range skill)
     :radius (get-skill-radius skill)
     :upper  (get-skill-limit-upper skill)
     :lower  (get-skill-limit-lower skill)}))

(defn skill-range-horizontal
  "Given a skill, returns the maximum and minimum distance it can reach on the x-z plane."
  [skill-or-id]
  (let [{:keys [shape range radius]} (get-skill-info skill-or-id)]
    (case shape
      :sphere   [0 (+ range radius)]
      :column   [(- range radius) (+ range radius)]
      :triangle [0 range])))

(defn skill-range-vertical
  "Given a skill, returns the maximum and minimum distance it can reach on the x-y plane."
  [skill-or-id]
  (let [{:keys [upper lower]} (get-skill-info skill-or-id)]
    [upper lower]))

(defn within-area? [pos min-pos max-pos]
  (every? true? (concat
                 (map >= pos min-pos)
                 (map <= pos max-pos))))

(defn within-sphere? [pos center radius]
  (<= (dist pos center) radius))

(defn within-cylinder? [pos center radius vert-range]
  (let [[x y z]       pos
        [cx cy cz]    center
        [min-y max-y] vert-range]
    (println (<= min-y y max-y)
             (dist x z cx cz)
             (<= (dist x z cx cz) radius)
             radius)
    (and (<= min-y y max-y)
         (<= (dist x z cx cz) radius))))

(defn skill-in-range?
  "Given a skill and unit, returns true if the skill's range and the unit's remaining move
  can '"
  [unit target skill-or-id]
  (let [[x-min x-max] (skill-range-horizontal skill-or-id)
        [y-min y-max] (skill-range-vertical skill-or-id)
        vert-range [(- (pos-y unit) y-min)
                    (+ (pos-y unit) y-max)]
        {:keys [shape range]} (get-skill-info skill-or-id)
        my-pos (get-pos unit)
        target-pos (get-pos target)]
    (println my-pos target-pos shape range vert-range x-min x-max)
    (case shape
      :sphere (within-cylinder? target-pos my-pos (+ x-max (get-remaining-move)) vert-range)
      :column false
      :triangle false)))

(defn-unit in-range? [unit target]
  (some (filter (partial skill-in-range? unit target) (get-skills))))

(defn dist-unit
  ([a b]          (dist (pos-x a) (pos-z a)
                        (pos-x b) (pos-z b)))
  ([unit]         (dist (pos-x unit) (pos-z unit)
                        (PSP/getPlayerX) (PSP/getPlayerZ))))

(defn dist
  ([x1 z1 x2 z2]  (dist [x1 0 z1]
                        [x2 0 z2]))
  ([[x1 y1 z1] [x2 y2 z2]]
   (Math/sqrt (+ (Math/pow (- x1 x2) 2)
                 (Math/pow (- y1 y2) 2)
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

(defn-unit closest [unit coll]
  (when (and unit (seq coll))
    (apply min-key (partial dist-unit unit) coll)))

(defn-unit nearby? [unit target-unit max-x max-y]
  (let [target-pos (get-pos target-unit)
        my-pos (get-pos unit)]
    (within-cylinder? target-pos my-pos
                      [max-x (- max-x)]
                      [max-y (- max-y)])))

(defn-unit units-nearby [unit range-x range-y coll]
  (let [comparator (fn [target-unit] (nearby? unit target-unit range-x range-y))
        nearby-units (remove #{unit} (filter comparator coll))]
    (set nearby-units)))

(def confine-radius 70)
(def confine-upper 1000)
(def confine-lower 1000)

(defn confine-targets []
  (let [nearby-items (units-nearby (active-unit) confine-radius (item-units))]
    (set (remove is-being-held? nearby-items))))

(defn-unit too-close? [unit target]
  (< (dist-unit unit target) 2.0))

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

(defn at-intrusion-stage?
  "Checks if the intrusion map screen is active."
  [] (PSP/isSpecialStageScreenUp))

(defn dead? [unit]
  (= 0 (.getCurrentHp unit)))

(defn can-enter-input? []
  (and (is-active?)
       (not (is-in-air?))))

(def selection-dist 2.0)

(defn move-to
  ([unit] (move-to unit selection-dist))
  ([unit within & [dir]]
   ;; TODO: adjust based on camera angle
   ;; TODO: calculate exact number of frames
   (let [comparator (if (= dir :away) < >)
         angle-fn (if (= dir :away) angle-away angle-to)]
     (while (comparator (dist-unit unit) within)
       (let [angle (mod (+ (angle-fn unit) 225) 360)
             scale (if (comparator (dist-unit unit) (+ 5.0 within)) 1.0 0.75)
             [ax ay] (angle->analog angle scale)]
         (play-input [[[:analog ax ay] 1]])
         ;; (step)
         )))))

(defn select-unit-in-cursor
  "When there are multiple units near the cursor, cycles to the given one."
  [unit]
  (when (selected-unit)
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
      (play-input
       (apply concat
              (concat
               (repeat presses (press :select 10))))))))

(defn select-unit
  "Moves the cursor to and selects the given unit."
  [unit]
  (move-to unit)
  (do-nothing menu-delay)
  (select-unit-in-cursor unit))
