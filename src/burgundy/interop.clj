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
(defn selected-unit-island [] (.getSelectedUnitIsland api))

(defn skill-types [] (.getSkillTypes api))

(defn island-menu-cursor [] (PSP/getIslandMenuCursorPos))
(defn status-menu-cursor [] (PSP/getStatusMenuCursorPos))
(defn battle-menu-cursor [] (PSP/getBattleMenuCursorPos))
(defn battle-unit-cursor [] (PSP/getBattleUnitMenuCursorPos))
(defn battle-attack-cursor [] (PSP/getBattleAttackMenuCursorPos))
(defn battle-confine-cursor [] (PSP/getConfineMenuCursorPos))
(defn marona-cursor [] (PSP/getMaronaMenuCursorPos))
(defn dungeon-menu-cursor [] (PSP/getDungeonMenuCursorPos))

(defn list-units [] (.listUnits api))
(defn summoned-units [] (PSP/getSummonedUnits))

(defn dungeons [] (.getDungeons api))
(defn generated-dungeon [] (.getGeneratedDungeon api))

(defn bol [] (.getBol api))

(defn contiguous-memory
  "Returns count arrays of size bytes starting at offset."
  [offset size count]
  (let [mem (PSP/readRam offset (* count size))
        objs (partition size mem)]
    (->> objs
         (map byte-array)
         (map bytes))))

(def button-masks
  {:wait      0x0000
   :square    0x8000
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
(def Ｌ [:ltrigger])
(def Ｒ [:rtrigger])

(def menu-delay 2)

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
     (zipmap
      (map #(.getID %) (seq ~coll))
      (map (comp (camel->keyword) #(.getName %)) (seq ~coll)))))

(defmacro gen-type-id-map [inject-sym coll]
  `(def ~inject-sym
     (clojure.set/map-invert ~coll)))

(defn gen-type-kw-maps []
  (gen-type-kw-map skill-type-kws (skill-types))
  (gen-type-id-map skill-type-ids skill-type-kws))

;; (play-input [:square 20] )

(defn press [buttons frames]
  (let [bitmask (button-bits buttons)]
    (dotimes [i frames]
      (step bitmask))))

(defn wait [frames]
  (press [] frames))

(defn push-analog [x y frames]
  (dotimes [i frames]
    (step x y)))

;; 40 [:a] [:a 1] [[:a :b] 1] [[:analog 1 2] 12] [[:a] [:a]]

(defn play-input [input]
  (doseq [i input]
    (let [buttons (if (vector? (first i)) (first i) [(first i)])]
      (cond
        (= 1 (count i))
        (do
          (press buttons 1)
          (wait menu-delay))

        (= (first i) :seq)
        (let [[input-list] (rest i)]
          (play-input input-list))

        :else
        (let [frames (second i)]
          (if (= (first buttons) :analog)
            (push-analog (nth buttons 1) (nth buttons 2) frames)
            (press buttons frames)))))))

;; (defn play-input
;;   "Sends input commands.
;;   Expects a vector of pairs of a vector of button keywords and the number of frames to hold them for."
;;   [input]
;;   (doseq [[buttons frames] input]
;;     (cond
;;       (keyword? buttons)
;;       (= (first buttons) :analog)
;;       (dotimes [i frames]
;;         (step (nth buttons 1) (nth buttons 2)))

;; :else (dotimes [i frames]
;;         (let [bitmask (button-bits buttons)]
;;           (step bitmask))))))

;; (defn do-nothing [frames]
;;   (play-input
;;    (wait frames)))

(defn print-flags [] (PSP/printFlags))
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


(defn player-x [] (PSP/getPlayerX))
(defn player-y [] (PSP/getPlayerY))
(defn player-z [] (PSP/getPlayerZ))

(defn player-pos []
  (let [x (player-x)
        y (player-y)
        z (player-z)]
    [x y z]))

(defn within-area? [pos min-pos max-pos]
  (every? true? (concat
                 (map >= pos min-pos)
                 (map <= pos max-pos))))

(defn within-sphere? [pos center radius]
  (<= (dist pos center) radius))

(defn within-cylinder? [pos center radius vert-range]
  (let [[x y z]       pos
        [cx cy cz]    center
        [max-y min-y] vert-range
        upper (+ y max-y)
        lower (- y min-y)]
    ;; (println (<= lower y upper)
    ;;          min-y y max-y
    ;;          (dist x z cx cz)
    ;;          (<= (dist x z cx cz) radius)
    ;;          radius)
    (and (<= lower y upper)
         (<= (dist x z cx cz) radius))))

(defn dist
  ([x z] (dist x z (player-x) (player-z)))
  ([x1 z1 x2 z2]  (dist x1 0 z1
                        x2 0 z2))
  ([x1 y1 z1 x2 y2 z2]
   (Math/sqrt (+ (Math/pow (- x1 x2) 2)
                 (Math/pow (- y1 y2) 2)
                 (Math/pow (- z1 z2) 2)))))

(defn deg->rad [deg] (* deg (/ Math/PI 180)))
(defn rad->deg [rad] (mod (* rad (/ 180 Math/PI)) 360))

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

(defn camera-rot [] (rad->deg (PSP/getCameraRot)))

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

(defn can-throw?
  [] (PSP/canThrow))

(defn stage-started?
  "Checks if a stage has started."
  [] (PSP/hasStageStarted))

(defn stage-clear?
  "Checks if the result screen after clearing a stage is currently active."
  [] (PSP/isStageClear))

(defn at-intrusion-stage?
  "Checks if the intrusion map screen is active."
  [] (PSP/isSpecialStageScreenUp))
