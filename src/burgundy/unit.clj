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

(defn-unit get-jump [unit] (.getJump unit))

(defn-unit get-held-unit [unit]
  (let [id (.getHeldItemID unit)]
    (if (= -1 id) nil
        (get-unit id))))

(defn has-moved? []
  (not= (get-max-move (active-unit)) (get-remaining-move (active-unit))))

(def no-move-threshold 5.0)

(defn has-move-remaining? []
  (> (get-remaining-move (active-unit)) no-move-threshold))

(defn-unit has-attacked? [unit] (.hasAttacked unit))

(defn-unit is-item? [unit] (.isItem unit))

(defn-unit is-holding? [unit] (not (nil? (get-held-unit unit))))
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

;; returns all skills, including passives and combos
;; which may not be usable at the time
(defn-unit get-skills [unit]
  (.getSkills unit))

(defn-unit get-usable-skills [unit]
  (let [skills (get-skills unit)
        actives (remove passive-skill? skills)
        holding? (is-holding? unit)
        being-held? (is-being-held? unit)
        held-chara? (and (not (is-item? unit))
                         being-held?)
        ]
    (as-> actives s
      (if held-chara? s (remove combo-skill? s))
      (if (or holding? being-held?) (remove unarmed-skill? s) (remove armed-skill? s)))))

(defn distinct-by
  "Returns a lazy sequence of the elements of coll, removing any elements that
  return duplicate values when passed to a function f."
  ([f]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result x]
          (let [fx (f x)]
            (if (contains? @seen fx)
              result
              (do (vswap! seen conj fx)
                  (rf result x)))))))))
  ([f coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                 ((fn [[x :as xs] seen]
                    (when-let [s (seq xs)]
                      (let [fx (f x)]
                        (if (contains? seen fx)
                          (recur (rest s) seen)
                          (cons x (step (rest s) (conj seen fx)))))))
                  xs seen)))]
     (step coll #{}))))

(defn-unit get-all-skills [unit]
  (distinct-by skill-id (concat (get-usable-skills unit)
                    (when (is-holding? unit)
                      (get-usable-skills (get-held-unit unit))))))

(defn get-skill-pos [skills skill]
  (let [skill-ids (map skill-id skills)
        id (skill-id skill)]
    (.indexOf skill-ids id)))

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
        vert-range (skill-range-vertical skill-or-id)
        {:keys [shape range]} (get-skill-info skill-or-id)
        my-pos (get-pos unit)
        target-pos (get-pos target)
        move (get-remaining-move unit)]
    (println (get-name unit) my-pos target-pos shape range vert-range x-min x-max)
    (println (skill-name skill-or-id))

    (case shape
      :sphere (within-cylinder? target-pos my-pos (+ x-max move) vert-range)
      ;;TODO: find better metric
      :column (and (is-single-target? skill-or-id)
                   (within-cylinder? target-pos my-pos (+ x-max move) vert-range))
      :triangle false)))

(defn-unit in-range? [unit target]
  (some (filter (partial skill-in-range? unit target)) (get-skills)))

(defn-unit should-use-skill? [unit skill]
  (and (or (is-spherical? skill)
           (is-single-target? skill))
       (not (unequip-skill? skill))
       (is-attack? skill)))

(defn-unit usable-skills [unit]
  (let [skills (get-all-skills unit)
        sp     (get-sp unit)]
    (filter (every-pred
             (partial can-use-skill? sp)
             (partial should-use-skill? unit))
            skills)))

(defn-unit skills-reaching [unit target]
  (let [skills (usable-skills unit)]
    (filter (partial skill-in-range? unit target) skills)))

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
     [px pz])))


(defn-unit closest [unit coll]
  (when (and unit (seq coll))
    (apply min-key (partial dist-unit unit) coll)))

(defn-unit nearby? [unit target-unit radius vert-range]
  (let [target-pos (get-pos target-unit)
        my-pos (get-pos unit)]
    (within-cylinder? target-pos my-pos
                      radius
                      vert-range)))

(defn-unit units-nearby [unit radius vert-range coll]
  (let [comparator (fn [target-unit] (nearby? unit target-unit radius vert-range))
        nearby-units (remove #{unit} (filter comparator coll))]
    (set nearby-units)))

(def confine-radius 70)
(def confine-upper 1000)
(def confine-lower 1000)

(defn-unit confine-targets [unit]
  (let [nearby-items (units-nearby unit confine-radius [confine-upper confine-lower] (item-units))]
    (set (remove is-being-held? nearby-items))))

(defn-unit too-close? [unit target]
  (< (dist-unit unit target) 2.0))

(defn can-enter-input? []
  (and (is-active?)
       (not (is-in-air?))))

(def selection-dist 0.5)

(def camera-angle 225)

(defn move-to
  ([unit] (move-to unit selection-dist))
  ([unit within & [dir]]
   ;; TODO: adjust based on camera angle
   ;; TODO: calculate exact number of frames
   (let [comparator (if (= dir :away) < >)
         angle-fn (if (= dir :away) angle-away angle-to)]
     (while (comparator (dist-unit unit) within)
       (let [angle (mod (+ (angle-fn unit) camera-angle) 360)
             scale (if (comparator (dist-unit unit) (+ 5.0 within)) 1.0 0.75)
             [ax ay] (angle->analog angle scale)]
         (play-input [[[:analog ax ay] 1]])
         )))))

(defn move-towards
  ([unit] (let [[x _ z] (get-pos unit)]
            (move-towards x z)))
  ([x z]
   (let [angle (mod (+ (angle-to x z (player-x) (player-z)) camera-angle) 360)
         scale (if (> (dist x z (player-x) (player-z)) 5) 1.0 0.75)
         [ax ay] (angle->analog angle scale)]
     (play-input [[[:analog ax ay] 1]]))))

(defn move-to-unit [unit]
  (when (or (nil? (selected-unit))
            (not= (get-id (selected-unit)) (get-id unit)))
    (move-towards unit)
    (recur unit)))

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
  (move-to-unit unit)
  (do-nothing 10)
  (select-unit-in-cursor unit))
