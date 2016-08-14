(ns burgundy.fusion
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.menu :refer :all]
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

(defn fusion-cost-diff [target material skill-kw compat fusionist-lv]
  (let [skill (get-skill-type (skill-kw skill-type-ids))
        cost (skill-fusion-cost target material skill :sss fusionist-lv)
        target-mana (get-mana target)
        diff (- target-mana cost)]
    diff))

(defn select-fusion-units [a b]
  (let [pos-a (menu-pos a)
        pos-b (menu-pos b)
        chara-count (count (island-charas))
        item-count (count (island-items))
        switch-first (and (is-item? b) (not (is-item? a)))
        switch-second (or switch-first
                          (and (is-item? a) (not (is-item? b))))
        size-a (if (is-item? a) item-count chara-count)
        size-b (if (is-item? b) item-count chara-count)]
    (play-input
     [(if switch-first △ [:wait]) [:wait 10]
      (menu-key-seq (fusion-menu-cursor) pos-a :fusion size-a)
      [:wait 10]×
      (if switch-second △ [:wait]) [:wait 10]
      (menu-key-seq (fusion-menu-cursor) pos-b :fusion size-b)
      [:wait 10]×])))
