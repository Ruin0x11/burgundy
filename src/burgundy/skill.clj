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

(defn can-use-skill?
  "Given a map of sp values and a skill, determines if the skill can be used.
  It takes SP instead of a unit because the skill owner might be an item, not the user."
  [all-sp skill-or-id]
  (do (let [sp-type (skill-sp-type skill-or-id)
            sp (sp-type all-sp)]
        (> sp (skill-sp-cost skill-or-id)))))

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
