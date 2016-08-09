(ns burgundy.battle
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]
            [burgundy.task :refer :all]
            [burgundy.queue :refer :all]
            [clojure.set :refer [intersection]]))

(def battle-state (ref {:summoned-units 0} ))

(def-task end-action-task []
  :desc ["Ending action."]
  :priority 9999
  :max-attempts 1
  :goal-state false
  ;;TODO: detect if changing units
  :action (end-action))

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
              (dosync
               (commute battle-state assoc-in [:summoned-units] (summoned-units))
               (commute battle-state assoc-in [:enemy-units] (count (enemy-units))))))

(def-task finish-stage-task []
  :desc ["Finishing stage."]
  :priority 0
  :goal-state (not (stage-clear?))
  :action (finish-stage))

(def-task confine-task [id target]
  :desc ["Confine unit " id " to " (get-name target)]
  :priority 10
  :max-attempts 3
  :goal-state (> (summoned-units) (:summoned-units @battle-state))
  :action (confine-unit target id)
  :on-success (dosync
               (commute battle-state assoc-in [:summoned-units] (summoned-units))))

(def-task confine-closest-task [id]
  :desc ["Confinining unit " id " to a nearby item."]
  :priority 10
  :max-attempts 3
  :goal-state (not (nil? result))
  :action (let [targets (confine-targets)
                selected (closest targets)]
            selected)
  :on-success (add-task (confine-task 0 result))
  )

(def-task confine-near-task [id target]
  :desc ["Confining unit " id " near " (get-name target)]
  :priority 25
  :max-attempts 3
  :goal-state (not (nil? result))
  :action (let [confinable (confine-targets)
                confinable-near-target (intersection confinable
                                                     (units-nearby target confine-radius (item-units)))
                selected (closest target confinable-near-target)]
            (println "Selected " (get-name selected))
            selected)
  :on-success (add-task (confine-task 0 result)))

(def-task attack-task [target]
  :desc ["Attacking " (get-name target)]
  :priority 20
  :max-attempts 3
  :goal-state (or (has-attacked?)
                  (not (has-move-remaining?)))
  :action (attack target)
  :on-failure (when (is-marona?)
                (add-task (confine-near-task 0 target))))

(def-task attack-nearest-task []
  :desc ["Looking for nearest unit to attack."]
  :priority 20
  :max-attempts 3
  :goal-state (not (nil? result))
  :action (let [target (closest (enemy-units))]
            (cond (too-close? target)         (move-unit target 20.0 :away)
                  (not (in-range? target 30)) (move-unit target 10.0))
            (if (in-range? (active-unit) target 30)
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
                 (< (summoned-units) 6))
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
