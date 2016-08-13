(ns burgundy.battle
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.skill :refer :all]
            [burgundy.menu :refer :all]
            [burgundy.task :refer :all]
            [burgundy.queue :refer :all]
            [clojure.set :refer [intersection]]))

(def units-to-summon 6)

(def battle-state (ref {:confined-units 0 :attempted-skills []
                        :attempted-confine-targets []} ))

(def-task end-action-task []
  :desc ["Ending action."]
  :priority 9999
  :max-attempts 1
  ;;TODO: detect if changing units
  :goal-state false
  :action (end-action)
  :on-failure (dosync
               (commute battle-state assoc-in [:attempted-skills] [])))

(def-task intrusion-stage-task []
  :desc ["Confirming intrusion stage screen."]
  :priority 0
  :goal-state (stage-started?)
  :action (intrusion-stage))

(def-task start-stage-task []
  :desc ["Starting stage."]
  :priority 1
  :goal-state (not (stage-started?))
  :action (do (start-stage)
              (load-skill-types)
              (dosync
               (commute battle-state assoc-in [:confined-units] (confined-units))
               (commute battle-state assoc-in [:attempted-skills] [])
               (commute battle-state assoc-in [:attempted-confine-targets] [])
               (commute battle-state assoc-in [:enemy-units] (count (enemy-units)))

               )))

(def-task finish-stage-task []
  :desc ["Finishing stage."]
  :priority 0
  :goal-state (not (stage-clear?))
  :action (finish-stage))

(def-task confine-task [id target]
  :desc ["Confine unit " id " to " (get-name target)]
  :priority 10
  :max-attempts 3
  :goal-state (> (confined-units) units-to-summon)
  :action (confine-unit target id)
  :on-success (dosync
               (commute battle-state assoc-in [:confined-units] (confined-units))))

(def-task confine-closest-task [id]
  :desc ["Confinining unit " id " to a nearby item."]
  :priority 10
  :max-attempts 3
  :goal-state (> (confined-units) (:confined-units @battle-state))
  :action (let [all-targets (confine-targets)
                attempted (:attempted-confine-targets @battle-state)
                targets (remove #(some #{(get-id %)} attempted) all-targets)
                selected (closest targets)]
            (confine-unit selected id)
            (dosync
             (commute battle-state assoc-in [:attempted-confine-targets] (conj attempted (get-id selected)))))
  :on-success (dosync
               (commute battle-state assoc-in [:confined-units] (confined-units)))
  )

(def-task confine-near-task [id target]
  :desc ["Confining unit " id " near " (get-name target)]
  :priority 25
  :max-attempts 3
  :goal-state (not (nil? result))
  :action (let [confinable (confine-targets)
                confinable-near-target (intersection confinable
                                                     (confine-targets target))
                selected (closest target confinable-near-target)]
            (println "Selected " (get-name selected))
            selected)
  :on-success (add-task (confine-task 0 result)))

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

(def-task attack-task [target]
  :desc ["Attacking " (get-name target)]
  :priority 20
  :max-attempts 3
  :goal-state (has-attacked?)
  :action (let [attempted (:attempted-skills @battle-state)
                skills (remove #(some #{(skill-id %)} attempted) (skills-reaching target))
                skill (skill-pos-for-target target skills)]
            (println attempted)
            (println (map skill-id skills))
            (when-not (empty? skills)
              (attack target skill (get-all-skills))
              (println (skill-name skill))
              (dosync
               (commute battle-state assoc-in [:attempted-skills] (conj attempted (skill-id skill))))))
  :on-failure (when (is-marona?)
                (add-task (confine-near-task 0 target))))

(def-task attack-nearest-task []
  :desc ["Looking for nearest unit to attack."]
  :priority 20
  :max-attempts 3
  :goal-state (not (nil? result))
  :action (let [target (closest (enemy-units))]
            (cond (too-close? target)         (move-unit target 20.0 :away)
                  (not (in-range? target)) (move-unit target 10.0))
            (if (in-range? target)
              target nil))
  :on-success (add-task (attack-task result)))

(defn update-battle-engine []
  (cond
    (at-intrusion-stage?)
    (add-task (intrusion-stage-task))

    (stage-started?)
    (add-task (start-stage-task))

    (stage-clear?)
    (add-task (finish-stage-task))

    :else
    (do
      (when (and (is-marona?)
                 (< (confined-units) 6))
        (add-task (confine-closest-task 0) battle-tasks 3))
      (when (> (count (enemy-units)) 0)
        (add-task (attack-nearest-task)))
      (add-task (end-action-task))))
  (list-tasks))

(defn run-battle-engine []
  (if (empty? @battle-tasks)
    (update-battle-engine)
    (when (not (empty? @battle-tasks))
      (let [task (first (dequeue! battle-tasks))]
        (try
          (run-task task)
          (catch Exception e
            (println "Exception in battle engine!")
            (.printStackTrace e))))
      (list-tasks)
      (recur))))
