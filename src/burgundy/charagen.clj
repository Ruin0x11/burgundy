(ns burgundy.charagen
  (:require  [burgundy.interop :refer :all]
             [burgundy.menu :refer :all]
             [burgundy.types :refer :all]
             [burgundy.unit :refer :all]))

(def passive-skill-classes
  {:big-bang      :titlist
   :parting-gift  :death-corgi
   :exp-riser     :dungeonmonk
   :mana-monger   :fusionist
   :made-of-money :merchant
   :healing-birth :healer
   :healing-steps :knight})

(defn soul-with-title [title]
  (let [units (neutral-units)
        titles (map (comp get-name get-title) units)]
    (.indexOf titles title)))

(defn soul-with-class [class]
  (let [units (neutral-units)
        classes (map get-class units)]
    (.indexOf classes class)))
