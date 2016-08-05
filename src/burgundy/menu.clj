(ns burgundy.menu
  (:require [burgundy.interop :refer :all])
  )

(def menu-scroll-amounts
  {:attack 8
   :confine 5
   :status 7})

(def menu-sizes
  {:attack 999
   :battle-unit 5 ;; plus one if marona
   :battle-main 4
   :confine 999
   :status 999})

(defn menu-key-seq
  "Calculates an input sequence to traverse a menu from position start to position end."
  ([start end](println (str start " " end))(let [diff (Math/abs (- start end))
                                                 amount (if (> diff (Math/floor (/ 6 2))) (- diff (+ 1 6)) diff)]
                                             (cond
                                               (< start end) (apply concat
                                                                    (concat (repeat diff [[[:down] 1] (wait menu-delay)])))
                                               :else         (apply concat
                                                                    (concat (repeat diff [[[:up] 1] (wait menu-delay)]))))))
  ([start end menu-type]
   (let [scroll-amt (get menu-scroll-amounts menu-type)
         diff (Math/abs (- start end))
         triggers (/ diff scroll-amt)
         arrows (mod diff scroll-amt)]
     (cond
       (< start end) (apply concat
                            (concat (repeat triggers [[[:rtrigger] 1] (wait menu-delay)])
                                    (repeat arrows [[[:down] 1] (wait menu-delay)])))
       :else         (apply concat
                            (concat (repeat triggers [[[:ltrigger] 1] (wait menu-delay)])
                                    (repeat arrows [[[:up] 1] (wait menu-delay)])))))))


(defn wait-until-active
  "Bad. Use state instead."
  []
  (while (not (is-active?))
    (println "--Waiting.--")
    (print-flags)
    (step)))

(defn cancel []
  (println "cancel")
  (play-input
   (press :circle)))

(defn look-for-walkable
  "Moves the cursor towards the unit until it finds a piece of terrain it can walk towards, then moves there.

  Only to be called at the move menu."
  [unit]
  (if (> (dist unit) 1.0)
    (if (can-move?)
      (do
       (play-input
        (press :cross))
       (wait-until-active))
      (let [angle (mod (+ (angle-to unit) 225) 360)
            [ax ay] (angle->analog angle 1.0)]
        (println [ax ay])
        (play-input [[[:analog ax ay] 1]])
        (recur unit)))
    (do
      (cancel)
      (cancel)
      )))

(defn move-unit [target dist & [dir]]
  (println "Moving.")
  (let [angle 90
        [ax ay] (angle->analog angle 1.0)]
    (play-input
     (concat
      (press :cross)
      (menu-key-seq (battle-unit-cursor) 0)
      (press :cross)))

    (move-to target 10.0 dir)

    (if (can-move?)
      (do
        (play-input
         (press :cross 20))
        (wait-until-active))
      ;; TODO: fix.
      (look-for-walkable (first (my-units))))
    (println "Moving ended.")))

(defn move-unit-quick
  [unit target dist & [dir]]
  (println "Moving quickly.")
  (move-to target dist dir)

  (play-input
   (concat
    [(wait 20)]
    (press :cross 20)
    (menu-key-seq (battle-unit-cursor) 0)))

  (if (can-move?)
    (do
      (play-input
       (press :cross 20))
      (wait-until-active))
    (look-for-walkable (first (my-units))))
  (println "Moving ended.")
  )

(defn attack [unit]
  (println "Take this.")
  (println (str "pos:" (battle-attack-cursor)))
  (move-to unit)
  (play-input
   (concat
    [(wait 10)]
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 1)
    (press :cross)))

  (play-input
   (concat
    (menu-key-seq (battle-attack-cursor) 0)
    (press :cross)))

  (if (can-attack?)
    (do
      (play-input
       (press :cross))
      (wait-until-active))
    (do
      (cancel)
      (cancel)
      (cancel)
      (move-unit unit 20 :away)))
  (println "Attack ended."))

(defn end-action []
  (println "Ending action.")
  (play-input
   (concat
    [(wait 4)]
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 5)
    (press :cross)))
  (println "Action ended.")
  )

(defn confine-unit [n]
  (play-input
   (concat
    [[[:cross] menu-delay]]
    (menu-key-seq (battle-unit-cursor) 3)
    [[[:cross] menu-delay]]
    (menu-key-seq (battle-confine-cursor) n :confine)
    [[[:cross] menu-delay]])))
