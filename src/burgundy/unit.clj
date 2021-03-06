(ns burgundy.unit
  (:require [burgundy.interop :refer :all]
            [burgundy.types :refer :all]
            [burgundy.skill :refer :all])
  (:import com.ruin.psp.PSP)
  (:import com.ruin.psp.models.Unit))

(defmacro defn-unit
  "Define a function that either takes a unit as the first argument, or takes one less argument and is applied to the active unit."
  [name args body]
  (let [other (rest args)]
    `(defn ~name
       ([~@other] (~name (active-unit) ~@other))
       (~args ~body))))

(defn-unit get-name [unit] (.getName unit))
(defn-unit get-id [unit] (if unit (.getID unit) -1))
(defn-unit get-identifier [unit] (.getIdentifier unit))
(defn-unit get-level [unit] (.getLevel unit))
(defn-unit get-mana [unit] (.getMana unit))
(defn-unit get-max-move [unit] (.getRemainingMove unit))
(defn-unit get-remaining-move [unit] (.getRemainingMove unit))
(defn-unit get-jump [unit] (.getJump unit))
(defn-unit get-steal [unit] (.getSteal unit))

(defn-unit get-title [unit] (.getTitle unit))
(defn-unit get-class [unit] (get class-type-kws (.getClassType unit)))

(defn-unit get-hp [unit] (.getHP unit))
(defn-unit get-current-hp [unit] (.getCurrentHP unit))
(defn-unit get-atk [unit] (.getAtk unit))
(defn-unit get-def [unit] (.getDef unit))
(defn-unit get-int [unit] (.getInt unit))
(defn-unit get-res [unit] (.getRes unit))
(defn-unit get-spd [unit] (.getSpd unit))

(defn-unit damaged? [unit] (> (- (get-hp unit) (get-current-hp unit)) 0))

(defn-unit pos-x [unit] (.getX unit))
(defn-unit pos-y [unit] (.getY unit))
(defn-unit pos-z [unit] (.getZ unit))

(defn-unit vel-x [unit] (.getVelX unit))
(defn-unit vel-y [unit] (.getVelY unit))
(defn-unit vel-z [unit] (.getVelZ unit))

(defn-unit is-item? [unit] (.isItem unit))

(defn island-items [] (filter is-item? (island-units)))
(defn island-charas [] (remove is-item? (island-units)))

(defn latest-chara [] (last (island-charas)))
(defn latest-item [] (last (island-items)))

(defn-unit menu-pos [unit] (.getMenuPos unit))

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

(defn-unit get-held-unit [unit]
  (let [id (.getHeldItemID unit)]
    (if (= -1 id) nil
        (get-unit id))))

(defn-unit has-attacked? [unit] (.hasAttacked unit))
(defn-unit holding? [unit] (not (nil? (get-held-unit unit))))
(defn-unit being-held? [unit] (.isBeingHeld unit))
(defn-unit my-unit? [unit] (.isFriendly unit))
(defn-unit is-marona? [unit] (= (get-name unit) "Marona"))
(defn dead? [unit] (= 0 (.getCurrentHP unit)))

(def no-move-threshold 5.0)

(defn has-moved? []
  (not= (get-max-move (active-unit)) (get-remaining-move (active-unit))))

(defn has-move-remaining? []
  (> (get-remaining-move (active-unit)) no-move-threshold))

(defn-unit dump [unit] (.dump unit))

(def unit-start-offset 0x01491090)
(def unit-size 2136)

(defn-unit unit-memory [unit]
  (seq (nth (contiguous-memory unit-start-offset unit-size 36) (get-id unit))))

(defn-unit unit-offset [unit]
  (+ unit-start-offset (* (get-id unit) unit-size)))

(defn-unit unit-byte [unit n]
  (nth (unit-memory unit) n))

(defn search-for-value [obj value]
  (let [bytes (if (instance? Unit obj) (unit-memory obj) obj)]
    (map (partial format "0x%04X") (positions #(= (byte value) %) bytes))))

;; returns all skills, including passives and combos
;; which may not be usable at the time
(defn-unit get-skills [unit]
  (map #(.getSkill api %) (.getSkills unit)))

(defn-unit get-usable-skills [unit]
  (let [skills (get-skills unit)
        actives (remove passive-skill? skills)
        holding? (holding? unit)
        being-held? (being-held? unit)
        held-chara? (and (not (is-item? unit))
                         being-held?)]
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
                                (when (holding? unit)
                                  (get-usable-skills (get-held-unit unit))))))

(defn-unit get-skill [unit skill-kw]
  (let [skills (get-skills unit)
        kws (map skill-keyword skills)
        idx (.indexOf kws skill-kw)]
    (when-not (= -1 idx)
      (nth skills idx))))

(defn get-skill-pos [skill skills]
  (let [skill-ids (map skill-id skills)
        id (skill-id skill)]
    (.indexOf skill-ids id)))

(defn-unit get-sp [unit]
  (do (zipmap (vals skill-sp-kws) (.getSp unit))))

(defn-unit get-max-sp [unit]
  (zipmap (vals skill-sp-kws) (.getMaxSp unit)))

(defn-unit get-sp-affinity [unit]
  (zipmap (vals skill-sp-kws) (.getSpAffinity unit)))

(defn-unit full-sp? [unit]
  (let [maxes (vals (get-max-sp unit))
        currents (vals (get-sp unit))]
    (every? true? (map = maxes currents))))

(defn-unit needs-heal? [unit]
  (not (full-sp? unit)))

(defn skill-in-range?
  "Given a skill, an attacking unit and a target unit, returns true if the skill's range and the unit's remaining move
  can reach the target."
  [unit target skill]
  (let [[x-min x-max] (skill-range-horizontal skill)
        vert-range (skill-range-vertical skill)
        {:keys [shape range]} (skill-details skill)
        my-pos (get-pos unit)
        target-pos (get-pos target)
        move (get-remaining-move unit)]
    ;; (println (get-name unit) my-pos target-pos shape range vert-range x-min x-max)
    ;; (println (skill-name skill))

    (case shape
      :sphere (within-cylinder? target-pos my-pos (+ x-max move) vert-range)
      ;;TODO: find better metric
      :column (and (is-single-target? skill)
                   (within-cylinder? target-pos my-pos (+ x-max move) vert-range))
      :triangle false)))

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

(defn-unit has-skill? [unit skill-kw]
  (let [skill-ids (map skill-id (get-all-skills unit))
        id (skill-kw skill-type-ids)]
    (in? skill-ids id)))

(defn-unit skills-reaching [unit target]
  (let [skills (usable-skills unit)]
    (filter (partial skill-in-range? unit target) skills)))

(defn-unit in-range? [unit target]
  (not (empty? (skills-reaching unit target))))

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

(defn unit-by-name [name]
  (first (filter #(= name (get-name %)) (units))))

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
    (set (remove being-held? nearby-items))))

(defn-unit too-close? [unit target]
  (< (dist-unit unit target) 2.0))

(defn can-enter-input? []
  (and (is-active?)
       (not (nil? (active-unit)))
       (not (is-in-air?))))

(defn-unit can-steal? [unit item]
  (let [my-lvl (get-level unit)
        my-steal (get-steal unit)
        item-lvl (get-level item)
        item-steal (get-steal item)
        raw-chance (- (* (/ my-steal 10) (+ 10 my-lvl))
                      (* (/ item-steal 10) (+ 10 item-lvl)))
        lvl-ratio (Math/ceil (/ item-lvl my-lvl))
        chance-mod (cond
                     (> lvl-ratio 8) 0
                     (<= 3) 1
                     :else (/ (- 1 lvl-ratio)))]
    (>= (* raw-chance chance-mod) 100)))

(def selection-dist 0.5)

(def camera-angle 180)

(defn move-to
  ([unit] (move-to unit selection-dist))
  ([unit within & [dir]]
   ;; TODO: adjust based on camera angle
   ;; TODO: calculate exact number of frames
   (let [comparator (if (= dir :away) < >)
         angle-fn (if (= dir :away) angle-away angle-to)]
     (while (comparator (dist-unit unit) within)
       (let [cam (+ 180 (camera-rot))
             angle (mod (+ (angle-fn unit) cam) 360)
             scale (if (comparator (dist-unit unit) (+ 5.0 within)) 1.0 0.75)
             [ax ay] (angle->analog angle scale)]
         (play-input [[[:analog ax ay] 1]])
         )))))

(defn move-towards
  ([unit] (let [[x _ z] (get-pos unit)]
            (move-towards x z)))
  ([x z]
   (let [cam (+ 180 (camera-rot))
         angle (mod (+ (angle-to x z (player-x) (player-z)) cam) 360)
         scale (if (> (dist x z (player-x) (player-z)) 5) 1.0 0.75)
         [ax ay] (angle->analog angle scale)]
     (play-input [[[:analog ax ay] 1]]))))

(defn move-to-point
  ([x z] (move-to-point x z 1.0))
  ([x z within]
   (when-not (< (dist x z) within)
     (move-towards x z)
     (recur x z within))))

(defn move-to-unit
  "Moves the cursor to the unit, accounting for stacked units and units that are held."
  [target & [type]]
  (move-towards target)
  (let [id (get-id target)
        selected (if (= type :island) (selected-unit-island) (selected-unit))
        cursor-units (units-under-cursor)]
    (if (nil? selected)
      (recur target [type])

      (when (and (not= (get-id selected) id)
                 (not (in? (map get-id cursor-units) id)))
        (let [held (get-held-unit selected)
              cursor-held-ids (map (comp get-id get-held-unit) cursor-units)]

          (when-not (nil? held)
            (println (get-id held)))

          (when (and (or (nil? held)
                         (not= (get-id held) id))
                     (not (in? cursor-held-ids id)))
            (do (println "not in cursor held or held")
                (recur target [type]))))))))

(defn select-unit-in-cursor
  "When there are multiple units near the cursor, cycles to the given one."
  [unit]
  (when (selected-unit)
    (let [cursor-units (units-under-cursor)
          cursor-held-units (map get-held-unit cursor-units)
          current (selected-unit-index)
          id (get-id unit)
          ;; by id, not exact copy
          ;; TODO: should this be the default?
          cursor-pos (.indexOf (map get-id cursor-units) id)
          held-pos (.indexOf (map get-id cursor-held-units) id)
          target (if (= -1 cursor-pos) held-pos cursor-pos)
          presses
          (cond
            (= target -1)      0
            (> current target) (+ target current)
            :else              (- target current))]
      (play-input
       (vec (repeat presses [:select 10])))))) 

(defn select-unit
  "Moves the cursor to and selects the given unit."
  [unit]
  (move-to-unit unit)
  (wait 10)
  (select-unit-in-cursor unit))

(defn print-units
  ([] (print-units (units)))
  ([units]
   (print (clojure.string/join "\n" (map str units)))))

(defn print-my-units []
  (print-units (my-units)))
