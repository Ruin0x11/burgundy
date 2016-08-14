(ns burgundy.fusion
  (:require [burgundy.interop :refer :all]
            [burgundy.skill :refer :all]))

(def fusion-compat-modifiers
  {:sss 1.0
   :ss  1.5
   :s   2.0
   :aa  2.5
   :a   3.0
   :b   3.5
   :c   4.0
   :d   4.5
   :e   5.0
   :f   5.5})

(defn skill-fusion-cost [target material skill compat fusionist-lv]
  (let [compatibility (compat fusion-compat-modifiers)
        skill-cost (skill-mana-cost skill)
        target-lvl (get-level target)
        material-lvl (get-level material)
        penalty (/ (+ target-lvl material-lvl) (* 2 (+ fusionist-lv 1)))]
    (* skill-cost compatibility penalty)))
