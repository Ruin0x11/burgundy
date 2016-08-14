(ns burgundy.skill
  (:require [burgundy.interop :refer :all]))

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

(def skill-shapes
  {0 :sphere
   1 :column
   2 :triangle})

(defn get-skill-type [skill-or-id]
  (.getSkillType api
                 (if (number? skill-or-id)
                   skill-or-id
                   (.getID skill-or-id))))

(defn skill-name [skill] (.getName skill))
(defn skill-id [skill] (.getID skill))
(defn skill-sp-cost [skill] (.getSpCost skill))
(defn skill-mana-cost [skill] (.getManaCost skill))
(defn skill-keyword [skill] (get skill-type-kws (.getID skill)))
(defn skill-attack-type [skill] (get skill-attack-types (.getAttackType skill)))
(defn skill-sp-type [skill] (get skill-sp-kws (.getSpType skill)))

(defn print-skill [skill]
  (println (.getName skill)
           (.getDesc skill)
           (.getSpCost skill)))

(defn can-use-skill?
  "Given a map of SP values and a skill, determines if the skill can be used.
  It takes SP instead of a unit because the skill owner might be an item, not the user."
  [all-sp skill]
  (do (let [sp-type (skill-sp-type skill)
            sp (sp-type all-sp)]
        (> sp (skill-sp-cost skill)))))

(defn skill-shape [skill] (get skill-shapes (.getShape skill)))
(defn skill-radius [skill] (.getRadius skill))
(defn skill-range [skill] (.getRange skill))
(defn skill-lower [skill] (.getLimitLower skill))
(defn skill-upper [skill] (.getLimitUpper skill))
(defn unequip-skill? [skill] (.isUnequip skill))

(defn skill-details [skill]
  {:keyword (skill-keyword skill)
   :attack-type (skill-attack-type skill)
   :sp-cost (skill-sp-cost skill)
   :sp-type (skill-sp-type skill)

   :shape  (skill-shape skill)
   :range  (skill-range skill)
   :radius (skill-radius skill)
   :upper  (skill-upper skill)
   :lower  (skill-lower skill)})

(def skill-equip-types
  {0 :free
   1 :armed
   2 :unarmed
   3 :combo
   4 :passive})

;; TODO: temporary
(defn is-spherical? [skill] (= :sphere (skill-shape skill)))

(defn skill-equip-type [skill] (get skill-equip-types (.getEquipType skill)))

;; skill equip type predicates (armed-skill?)
(doseq [v (vals skill-equip-types)]
  (intern *ns*
          (symbol (str (name v) "-skill?"))
          (fn [skill] (= v (skill-equip-type skill)))))

(defn is-attack? [skill]
  (let [type (skill-attack-type skill)
        kw (skill-keyword skill)]
    (and (not-any? #(= type %) [:recovery :support])
         (not (passive-skill? skill))
         ;;TODO: detect these
         (not-any? #(= kw %) [:return :toss :pass]))))

(defn is-heal? [skill]
  (let [type (skill-attack-type skill)]
    (= type :recovery)))

(defn is-single-target? [skill]
  (let [{:keys [shape radius]} (skill-details skill)]
    (and (= shape :column)
         (= radius 0))))

(defn skill-range-horizontal
  "Given a skill, returns the maximum and minimum distance it can reach on the x-z plane."
  [skill]
  (let [{:keys [shape range radius]} (skill-details skill)]
    (case shape
      :sphere   [0 (+ range radius)]
      :column   [(- range radius) (+ range radius)]
      :triangle [0 range])))

(defn skill-range-vertical
  "Given a skill, returns the maximum and minimum distance it can reach on the x-y plane."
  [skill]
  (let [{:keys [upper lower]} (skill-details skill)]
    [upper lower]))
