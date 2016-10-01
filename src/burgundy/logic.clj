(ns burgundy.logic
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.skill :refer :all]
            [burgundy.dungeon :refer :all]
            [burgundy.charagen :refer :all]
            [burgundy.fusion :refer :all]
            [burgundy.isle :refer :all]
            [burgundy.menu :refer :all]
            [alter-ego.core :refer :all])
  (:import (com.ruin.psp.models Unit)))

(defn retry
  [tries f & args]
  (let [res (apply f args)]
    (if (and (not res) (= 0 tries))
      false
      (if (not res)
        (recur (dec tries) f args)
        res))))

;; create references to character positions
;; when denoting the target of a goal, select a ref in the vector
;; whenever a unit is removed or added, the position of the ref will shift accordingly
;; that way the position can change inside goals
;; if the position becomes nil, it means the unit was deleted
(def item-refs (ref []))
(def chara-refs (ref []))
(def dungeon-refs (ref []))

(defn make-unit-refs []
  (dosync
   (ref-set item-refs (vec (map ref (range 0 (count (island-items))))))
   (ref-set chara-refs (vec (map ref (range 0 (count (island-charas))))))
   (ref-set dungeon-refs (vec (map ref (range 0 (count (dungeons))))))))

(defn delete-element [vc pos]
  (vec (concat
        (subvec vc 0 pos)
        (subvec vc (inc pos)))))

(defn add-unit [refs]
  (dosync
   (commute refs conj (ref (count @refs)))))

(defn unit-from-ref [ref units]
  (nth units @ref))

(defn ref-from-unit [unit refs]
  (nth @refs (menu-pos unit)))

(def logic-state
  ":team-members - units that will not be deleted
   :tags - allows selecting targets of actions, like what objects to fuse
   :goal - the thing the AI is trying to do"
  (ref {:team-members [] :tags {} :goals []}))

(def battle-state
  ":attempted-skills - skills that the current unit has tried and failed to apply
   :attempted-confine-targets - items that the unit has failed to confine to"
  (ref {:attempted-skills [] :attempted-confine-targets []}))

(defn team-members []
  (map #(unit-from-ref % (island-charas)) (get-in @logic-state [:team-members])))

(defn add-team-members []
  (make-unit-refs)
  (dosync (dorun
           (doseq [val (seq @chara-refs)]
             (commute logic-state update-in [:team-members] conj val)))))

(defn clean-tags [tags]
  (into {} (filter (comp deref first) tags)))

(defn clean-tagged-units
  "Removes units that have been deleted from the tags."
  []
  (dosync
   (commute logic-state update-in [:tags] clean-tags)))

(defn remove-unit [unit-or-pos refs]
  (let [n (if (instance? Unit unit-or-pos) (menu-pos unit-or-pos) unit-or-pos)]
    (dosync
     (ref-set (nth @refs n) nil)
     (commute refs delete-element n))
    (let [stale (drop n @refs)]
      (dosync (dorun
               (map #(commute % - 1) stale))))
    (clean-tagged-units)))

(defn make-chara-refs []
  (map ref (range 0 (count (island-charas)))))

(defn tag-unit [unit tag]
  (let [pos (menu-pos unit)
        refs (if (is-item? unit) item-refs chara-refs)
        unit-ref (nth @refs pos)]
    (dosync
     (commute logic-state assoc-in [:tags unit-ref] tag))))

(defn get-unit-tag [unit-or-pos]
  (let [pos (if (instance? Unit unit-or-pos) (menu-pos unit-or-pos) unit-or-pos)]
    (get-in @logic-state [:tags pos])))

(defn goals []
  (get-in @logic-state [:goals]))

(defn current-goal []
  (first (goals)))

(defn add-goal [goal]
  (println "ADDED GOAL: " + (:type goal))
  (let [goals (goals)]
    (dosync
     (commute logic-state assoc-in [:goals] (cons goal goals)))))

(defn update-goal [tag val]
  (dosync
   (commute logic-state assoc-in [:goals 1 tag] val)))

(defn end-goal []
  (dosync
   (commute logic-state update-in [:goals] next)))

(defn members-with-class [class]
  (filter #(= (get-class %) class) (team-members)))

(defn units-with-role [role]
  (map #(unit-from-ref (first %) (island-charas)) (filter #(= role (:role (val %))) (:tags @logic-state))))

(defn unit-with-role [role]
  (first (units-with-role role)))

(defn nobody-doing? [role]
  (empty? (units-with-role role)))

;; safe versions of actions that add and remove units
;; references to unit positions must be updated when the unit list is reorganized
;; otherwise the same unit will not be referenced anymore on a position change

(defn create-character-with-tag [class tag]
  (when (create-character class 0)
    (add-unit chara-refs)
    (tag-unit (latest-chara) tag)
    (:tags @logic-state)))

(defn fuse-safe [target material]
  (when (fuse target material)
    (remove-unit material chara-refs)))

(defn banish-safe [unit]
  (when (banish unit)
    (remove-unit unit chara-refs)))

(def ≫ sequence)
(def ？ selector)
;; (def λ action)

(defn should-heal? []
  (some #{true} (map needs-heal? (island-units))))

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

(defn get-mana-tree [target threshold]
  ;; go to dungeon with highest level possible
  ;; obtain items of highest mana
  ;; until there are no more materials:
  ;;   fuse material onto target, transferring mana
  ;; when target mana >= threshold, end goal
  )

(defn grind-passive-tree [identifier skill-kw]
  (let [fusionist-lv (get-level (first (members-with-class :fusionist)))
        fusion-target (unit-with-role :fusion-target)
        fusion-material (unit-with-role :fusion-material)]
    (？ "Obtain Passive Skill"
        (？ "Fuse to soldier"
            (action "Create Soldier" (when (nil? fusion-target)
                                       (create-character-with-tag :soldier {:role :fusion-target})
                                       true))

            (action "Create Target" (when (nil? fusion-material)
                                      (let [material-class (skill-kw passive-skill-classes)]
                                        (create-character-with-tag material-class {:role :fusion-material}))
                                      true))
            (action "Fuse" (do
                             (fuse-safe fusion-target fusion-material)
                             (end-goal))))
        (≫ "Fuse to beneficiary"
         (action "Level = 99?" (let [skill (get-skill fusion-material skill-kw)]
                                 (when skill
                                   (= 99 (get-level skill)))))
         (action "Enough Mana?"
                 (let [diff (fusion-cost-diff fusion-target fusion-material skill-kw :sss fusionist-lv)]
                   (when (> diff 0)
                     (add-goal {:type :get-mana :target (ref-from-unit fusion-target chara-refs) :threshold (+ (get-mana fusion-target) diff)})
                     false)))
         (action "Fuse" (do
                          #_(fuse-safe fusion-target fusion-material :skills [:return])
                          (end-goal))))
        )))


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

(defn choose-goal-tree []
  (let [goal (current-goal)]
    (case (:type goal)
      :grind-passive (grind-passive-tree (:target goal) (:skill goal))
      :get-mana (get-mana-tree (:target goal) (:threshold goal)))))

(defn island-tree []
  (？ "Island Tree"
      (≫ "Heal"
       (action "Should heal?" (should-heal?))
       (action "Heal party" (heal-party)
               true))
      (dungeon-tree perfect-title-dungeon)))

(def units-to-confine 4)

(defn confine-to-closest [id]
  (let [all-targets (confine-targets)
        attempted (:attempted-confine-targets @battle-state)
        targets (remove #(some #{(get-id %)} attempted) all-targets)
        selected (closest targets)]
    (confine-unit selected id)
    (dosync
     (commute battle-state assoc-in [:attempted-confine-targets] (conj attempted (get-id selected))))))

(defn confine-tree []
  (≫ "Confine Tree"
   (action "Units < Threshold?" (when (< (confined-unit-count) units-to-confine) true))
   (action "Confine" (do
                       (retry 3 confine-to-closest 0)
                       true))))

(defn attack-action [target]
  (let [attempted (:attempted-skills @battle-state)
        skills (remove #(some #{(skill-id %)} attempted) (skills-reaching target))
        skill (skill-pos-for-target target skills)]
    (println attempted)
    (println (map skill-id skills))
    (when-not (empty? skills)
      (attack target skill (get-all-skills))
      (println (skill-name skill))
      (dosync
       (commute battle-state assoc-in [:attempted-skills] (conj attempted (skill-id skill)))))))

(defn get-nearest-target []
  (let [target (closest (enemy-units))]
    (cond (too-close? target)         (move-unit target 20.0 :away)
          (not (in-range? target)) (move-unit target 10.0))
    (if (in-range? target)
      target nil)))

(defn attack-tree [])

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
      (confine-tree)
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
