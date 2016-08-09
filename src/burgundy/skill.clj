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

(defn skill-name [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getName skill)))

(defn skill-id [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getID skill)))

(defn skill-keyword [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (get skill-type-kws (.getID skill))))

(defn skill-attack-type [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (get skill-attack-types (.getAttackType skill))))

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

(defn can-use-skill?
  "Given a map of SP values and a skill, determines if the skill can be used.
  It takes SP instead of a unit because the skill owner might be an item, not the user."
  [all-sp skill-or-id]
  (do (let [sp-type (skill-sp-type skill-or-id)
            sp (sp-type all-sp)]
        (> sp (skill-sp-cost skill-or-id)))))

(defn get-skill-shape [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (get skill-shapes (.getShape skill))))

(defn get-skill-radius [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getRadius skill)))

(defn get-skill-range [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getRange skill)))

(defn get-skill-lower [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getLimitLower skill)))

(defn get-skill-upper [skill-or-id]
  (let [skill (get-skill-type skill-or-id)]
    (.getLimitUpper skill)))

(defn get-skill-info [skill-or-id]
  {:keyword (skill-keyword skill-or-id)
   :attack-type (skill-attack-type skill-or-id)
   :sp-cost (skill-sp-cost skill-or-id)
   :sp-type (skill-sp-type skill-or-id)

   :shape  (get-skill-shape skill-or-id)
   :range  (get-skill-range skill-or-id)
   :radius (get-skill-radius skill-or-id)
   :upper  (get-skill-upper skill-or-id)
   :lower  (get-skill-lower skill-or-id)})

;; TODO: temporary
(defn is-spherical? [skill-or-id]
  (= :sphere (get-skill-shape skill-or-id)))

(defn is-attack? [skill-or-id]
  (let [type (skill-attack-type skill-or-id)
        kw (skill-keyword skill-or-id)]
    (and (every? #(not= type %) [:recovery :support])
         ;;TODO: detect this
         (not= kw :return))))

(defn is-heal? [skill-or-id]
  (let [type (skill-attack-type skill-or-id)]
    (= type :recovery)))

(defn is-single-target? [skill-or-id]
  (let [{:keys [shape radius]} (get-skill-info skill-or-id)]
    (and (= shape :column)
         (= radius 0))))

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
