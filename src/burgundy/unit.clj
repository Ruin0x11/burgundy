(ns burgundy.unit
  (:require [burgundy.interop :refer :all]
            [burgundy.skill :refer :all])
  (:import com.ruin.psp.PSP))

(defmacro defn-unit
  "Define a function that either takes a unit argument, or takes no arguments and is applied to the active unit."
  [name args body]
  (let [other (rest args)]
    `(defn ~name
       ([~@other] (~name (active-unit) ~@other))
       (~args ~body))))

(defn-unit pos-x [unit] (.getX unit))
(defn-unit pos-y [unit] (.getY unit))
(defn-unit pos-z [unit] (.getZ unit))

(defn-unit vel-x [unit] (.getVelX unit))
(defn-unit vel-y [unit] (.getVelY unit))
(defn-unit vel-z [unit] (.getVelZ unit))

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

(defn-unit get-name [unit] (.getName unit))

(defn-unit get-id [unit] (.getID unit))

(defn-unit get-mana [unit] (.getMana unit))

(defn-unit get-max-move [unit] (.getRemainingMove unit))

(defn-unit get-remaining-move [unit] (.getRemainingMove unit))

(defn-unit get-held-unit [unit]
  (get-unit (.getHeldItemID unit)))
(defn has-moved? []
  (not= (get-max-move (active-unit)) (get-remaining-move (active-unit))))

(def no-move-threshold 5.0)

(defn has-move-remaining? []
  (> (get-remaining-move (active-unit)) no-move-threshold))

(defn-unit has-attacked? [unit] (.hasAttacked unit))

(defn-unit is-being-held? [unit] (.isBeingHeld unit))

(defn-unit is-friendly? [unit] (.isFriendly unit))

(defn-unit is-marona? [unit] (= (get-name unit) "Marona"))

(defn dead? [unit] (= 0 (.getCurrentHp unit)))

(defn-unit dump [unit] (.dump unit))

(def unit-start-offset 0x01491090)
(def unit-size 2136)

(defn-unit unit-memory [unit]
  (seq (nth (contiguous-memory unit-start-offset unit-size 36) (get-id unit))))

(defn-unit unit-offset [unit]
  (+ unit-start-offset (* (get-id unit) unit-size)))

(defn-unit unit-byte [unit n]
  (nth (unit-memory unit) n))


(defn-unit get-skills [unit]
  (.getSkills unit))

(defn-unit get-all-skills [unit]
  (concat (get-skills unit)
          (get-skills (get-held-unit unit))))

(defn-unit get-sp [unit]
  (do (zipmap (vals skill-sp-kws) (.getSp unit))))

(defn-unit get-max-sp [unit]
  (zipmap (vals skill-sp-kws) (.getMaxSp unit)))

(defn-unit get-sp-affinity [unit]
  (zipmap (vals skill-sp-kws) (.getSpAffinity unit)))

(defn skill-in-range?
  "Given a skill, an attacking unit and a target unit, returns true if the skill's range and the unit's remaining move
  can reach the target."
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

(defn-unit usable-skills [unit]
  (let [skills (get-all-skills unit)
        sp     (get-sp unit)]
    (filter (partial can-use-skill? sp) skills)))


(defn dist-unit
  ([a b]          (dist (pos-x a) (pos-z a)
                        (pos-x b) (pos-z b)))
  ([unit]         (dist (pos-x unit) (pos-z unit)
                        (PSP/getPlayerX) (PSP/getPlayerZ))))

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
