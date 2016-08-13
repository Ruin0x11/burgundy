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
   :tags - allows selecting targets of actions, like what objects to fuse"
  (ref {:team-members [] :tags {}}))

(defn tag-unit [unit-or-identifier tag]
  (let [identifier (if (instance? Unit unit-or-identifier) (get-identifier unit-or-identifier) unit-or-identifier)]
    (dosync
     (commute logic-state assoc-in [:tags identifier] tag))))

(defn get-unit-tag [unit-or-identifier]
  (let [identifier (if (instance? Unit unit-or-identifier) (get-identifier unit-or-identifier) unit-or-identifier)] 
    (get-in @logic-state [:tags identifier])))

(defn vvals [m]
  (when (map? m) (vals m)))

(defn units-with-role [role]
  (filter #(= role (:role (val %))) (:tags @logic-state)))

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

(defn island-tree []
  (？ "Island Tree"
      (≫ "Heal"
       (action "Should heal?" (should-heal?))
       (action "Heal party" (heal-party)
               true))
      (？ "Dungeon"
          (action "Delete Dungeon"
                  (when (>= (count (dungeons)) max-dungeons)
                    (delete-dungeon 0)
                    true))
          (action "Create Dungeon"
                  (when (= 0 (count (dungeons)))
                    (create-dungeon perfect-title-dungeon)
                    true))
          (action "Go to Dungeon"
                  (go-to-dungeon (count (dungeons)))
                  true))))

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

(defn step-tree []
  (exec (root)))
