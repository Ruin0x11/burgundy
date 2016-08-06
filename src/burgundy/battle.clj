(ns burgundy.battle
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]))

(defn run-battle-engine []
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
          (and (is-marona? (active-unit))
               (< (summoned-units) 6))
          (let [targets (confine-targets)
                selected (closest targets)]
            (println (str " *** Trying to summon on " (get-name selected)))

            (confine-unit selected 9))

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
