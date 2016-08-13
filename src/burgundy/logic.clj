(ns burgundy.logic
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.dungeon :refer :all]
            [burgundy.isle :refer :all]
            [burgundy.menu :refer :all]
            [alter-ego.core :refer :all]))

(def ≫ sequence)
(def ？ selector)
;; (def λ action)

(defn should-heal? []
  (some #{true} (map needs-heal? (island-charas))))

(defn island-tree []
  (？ "Island Tree"
      (≫ "Heal"
       (action "Should heal?" (should-heal?))
       (action (heal-party)
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
