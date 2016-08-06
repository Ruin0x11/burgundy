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
        (cond
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
        (println (str "clear:" (stage-clear?)))
        (when (not (stage-clear?))
          (end-action))
        (wait-until-active))

      (stage-clear?)
      (finish-stage))))
