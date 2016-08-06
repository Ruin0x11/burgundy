(ns burgundy.battle
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]))

(defn run-battle-engine []
  (let [the-unit (first (my-units))
        target (closest the-unit (enemy-units))]
    (cond
      (at-special-stage?)
      (special-stage)

      (stage-started?)
      (start-stage)

      (not (or (nil? target) (nil? the-unit)))
      (do
        (when (> (dist the-unit) 10.0)
          (cancel))
        (when (not= (selected-unit) (active-unit))
          (select-unit-in-cursor (active-unit)))
        (cond
          (and (is-marona? (active-unit))
               (< (summoned-units) 3))
          (let [targets (confine-targets)
                selected (closest targets)]
            (println (get-name selected))
            (when
                (too-close? (active-unit) selected)
              (move-unit target 10.0 :away))
            (confine-unit selected 7))

          (too-close? the-unit target)
          (move-unit target 20.0 :away)

          (in-range? the-unit target 20)
          (do
            (println (dist the-unit target))
            (println (get-pos the-unit))
            (println (get-pos target))
            (attack target))
          :else
          (do
            (println (dist the-unit target))
            (println (get-pos the-unit))
            (println (get-pos target))
            (move-unit (closest (first (my-units)) (enemy-units)) 10.0)
            (when (in-range? (first (my-units)) target 20)
              (attack target))))
        (when (not (stage-clear?))
          (end-action))
        (wait-until-active))

      (stage-clear?)
      (finish-stage))))
