(ns burgundy.battle
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]
            [burgundy.task :refer :all]
            [burgundy.queue :refer :all]
            [clojure.set :refer [intersection]]))

(defn run-battle-engine []
  (if (empty? @battle-tasks)
    ;; (println "Nothing to do...")
    "nope"
    (let [task (first (dequeue! battle-tasks))]
      (try
        (println (str "====Task " (:name task) " started.===="))
        (run-task task)
        (println (str "====Task " (:name task) " ended.===="))
        (catch Exception e
          (println "Exception in battle engine!")
          (.printStackTrace e)))
      (recur))))

(def-task attack-nearest-task []
  :desc ["Looking for nearest unit to attack."]
  :priority 20
  :max-attempts 3
  :goal-state (has-attacked? (active-unit))
  :action (let [target (closest (enemy-units))]
            (cond (too-close? target)         (move-unit target 20.0 :away)
                  (not (in-range? target 10)) (move-unit target 10.0))
            (when (in-range? (active-unit) target 10)
              (attack target))))

(def-task confine-near-unit-task [id target]
  :desc ["Confining a unit near " (get-name target)]
  :priority 25
  :max-attempts 3
  :goal-state (> (summoned-units) 4)
  :action (let [confinable (confine-targets)
                confinable-near-target (intersection confinable
                                                          (units-nearby target confine-radius (item-units)))
                selected (closest target confinable-near-target)]
            (println "Selected " (get-name selected))
            (confine-unit selected id)))

(def-task confine-task [id unit]
  :desc ["Confine unit " id " to " (get-name unit)]
  :priority 10
  :max-attempts 3
  :goal-state (> (summoned-units) 6)
  :action (let [targets (confine-targets)
                selected (closest targets)]
            (confine-unit selected id)))

(defn run-battle-engine-old
  []
  (let [target (closest (active-unit) (enemy-units))]
    (cond
      (at-special-stage?)
      (special-stage)

      (stage-started?)
      (start-stage)

      (not (or (nil? target) (nil? (active-unit))))
      (do
        (when (> (dist (active-unit)) 10.0)
          (cancel))

        (cond
          (too-close? (active-unit) target)
          (do
            (println " *** Running away")
            (move-unit target 20.0 :away))

          (in-range? (active-unit) target 3)
          (do
            (println " *** Attacking")
            (println (dist (active-unit) target))
            (println (get-pos (active-unit)))
            (println (get-pos target))
            (attack target))
          :else
          (do
            (println " *** Moving & Attacking")
            (println (dist (active-unit) target))
            (println (get-pos (active-unit)))
            (println (get-pos target))
            (move-unit (closest (active-unit) (enemy-units)) 10.0)
            (when (in-range? (active-unit) target 20)
              (attack target))))
        (when (not (stage-clear?))
          (end-action))
        (wait-until-active))

      (stage-clear?)
      (finish-stage))))
