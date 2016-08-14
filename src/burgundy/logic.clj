(ns burgundy.logic
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.dungeon :refer :all]
            [burgundy.isle :refer :all]
            [burgundy.menu :refer :all]
            [alter-ego.core :refer :all])
  (:import (com.ruin.psp.models Unit)))

(def logic-state
  ":team-members - units that will not be deleted
   :tags - allows selecting targets of actions, like what objects to fuse
   :goal - the thing the AI is trying to do"
  (ref {:team-members [] :tags {}}))

(defn tag-unit [unit-or-identifier tag]
  (let [identifier (if (instance? Unit unit-or-identifier) (get-identifier unit-or-identifier) unit-or-identifier)]
    (dosync
     (commute logic-state assoc-in [:tags identifier] tag))))

(defn get-unit-tag [unit-or-identifier]
  (let [identifier (if (instance? Unit unit-or-identifier) (get-identifier unit-or-identifier) unit-or-identifier)] 
    (get-in @logic-state [:tags identifier])))

(defn team-members []
  (map get-island-unit (:team-members @logic-state)))

(defn member-with-class [class]
  (some #{class} (map get-class (team-members))))

(defn vvals [m]
  (when (map? m) (vals m)))

(defn units-with-role [role]
  (filter #(= role (:role (val %))) (:tags @logic-state)))

(defn nobody-doing? [role]
  (empty? (units-with-role role)))

(def ≫ sequence)
(def ？ selector)
;; (def λ action)

(defn should-heal? []
  (some #{true} (map needs-heal? (island-units))))

(defn latest-chara []
  (last (remove is-item? (island-units))))

(defn latest-item []
  (last (filter is-item? (island-units))))

(defn create-character-with-tag [class tag]
  (when (create-character class 0)
    (tag-unit (latest-chara) tag)))

(defn grind-sp-tree [unit sp-type threshold]
  ;; create high affinity char
  ;; equip best item with 0-sp skill
  ;; go to failure dungeon
  ;; attack units with 0-sp skill
  ;; when removed, return
  ;; if sp >= threshold, fuse to unit, end goal
  )

(defn fuse-item-tree [item stat threshold]
  ;; go to dungeon with highest level possible containing target item of high stat equip%
  ;; obtain items of highest mana and of target item type
  ;; until there are no more materials:
  ;;   if equip% of a material <= item stat, fuse items
  ;;   otherwise, fuse gathered materials with closely matching equip%
  ;; when equip% >= threshold, end goal
  )

(defn unlock-class-tree [class]
  ;; if class can't be created:
  ;;   go to dungeon containing class
  ;;   kill units of that class
  ;;   when 20 units have been killed, end goal
  )

(defn obtain-passive-tree [identifier skill-kw]
  (let [fusionist-lv (get-level (member-with-class :fusionist))
        ]
   (？ "Obtain Passive Skill"
       ;; create soldier
       (action "Create Soldier" (when (nobody-doing? :fusion-target)
                                 (create-character-with-tag :soldier {:role :fusion-target})
                                 true))

  ;; if not enough mana for fusion:
       (action "Get Mana" )
       ))
  ;;   go to failure dungeon
  ;;   take home high mana items
  ;;   fuse to soldier
  ;; create target unit
  ;; fuse passive to soldier
  ;; if passive level = 99:
  ;;   ensure mana, as above
  ;;   fuse to target, end goal
  )


(defn dungeon-tree [dungeon-type]
  (？ "Dungeon"
      (action "Delete Dungeon"
              (when (>= (count (dungeons)) max-dungeons)
                (delete-dungeon 0)
                true))
      (action "Create Dungeon"
              (when (= 0 (count (dungeons)))
                (create-dungeon dungeon-type)
                true))
      (action "Go to Dungeon"
              (go-to-dungeon (count (dungeons)))
              true)))

(defn island-tree []
  (？ "Island Tree"
      (≫ "Heal"
       (action "Should heal?" (should-heal?))
       (action "Heal party" (heal-party)
               true))
      (dungeon-tree perfect-title-dungeon)))

(defn battle-tree []
  (？ "Battle Tree"
      (action "Intrusion stage?" (when (at-intrusion-stage?)
                                   (intrusion-stage)
                                   true))
      (action "Stage started?" (when (stage-started?)
                                 (start-stage)
                                 (load-skill-types)
                                 true))
      (action "Stage clear?" (when (stage-clear?)
                               (finish-stage)
                               true))
      (action "Confine" true)
      (action "Attack" true))
  )

(defn root []
  (？ "Area"
      (≫ "Island"
       (action "On island?" (on-island?))
       (island-tree))
      (≫ "Battle"
       (battle-tree))))

(defn assert-assumptions []
  (assert (empty? (dups (map get-identifier (island-units))))))

(defn step-tree []
  (assert-assumptions)
  (exec (root)))
