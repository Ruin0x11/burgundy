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

(def menu-battle-unit
  {:move 0
   :attack 1
   :throw 2
   :confine 3
   :status 4
   :end-action 5})

(defn menu-key-seq
  "Calculates an input sequence to traverse a menu from position start to position end."
  ([start end] (menu-key-seq start end :menu))
  ([start end menu-type]
   (println (str start " " end))
   (let [size (menu-type menu-sizes)
         diff (Math/abs (- start end))
         amount
         (if (and (= menu-type :battle-unit)
                  (not (is-marona? (active-unit)))
                  (> end 4))
           (- diff 1) diff)]
     (cond
       (< start end) (apply concat
                            (concat (repeat amount (press :down))))
       :else         (apply concat
                            (concat (repeat amount (press :up))))))))


(defn wait-until-active
  []
  (while (not (is-active?))
    (println "--Waiting.--")
    (step)))

(defn cancel []
  (println "cancel")
  (play-input
   (press :circle)))

(defn select-active
  "Try to select the active unit."
  []
  (if (> (dist (active-unit)) selection-dist)
    (cancel))
  (select-unit-in-cursor (active-unit)))

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
  (select-active)
    (play-input
     (concat
      (press :cross)
      (menu-key-seq (battle-unit-cursor) 0 :battle-unit)
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
    (menu-key-seq (battle-unit-cursor) 0 :battle-unit)))

  (if (can-move?)
    (do
      (play-input
       (press :cross 20))
      (wait-until-active))
    (look-for-walkable (first (my-units))))
  (println "Moving ended.")
  )

(defn attack [target]
  (println "Take this.")
  (println (str "pos:" (battle-attack-cursor)))
  (select-unit target)
  (play-input
   (concat
    [(wait 10)]
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 1 :battle-unit)
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
      (move-unit target 20 :away)))
  (println "Attack ended."))

(defn end-action []
  (println "Ending action.")
  (select-active)
  (play-input
   (concat
    [(wait 4)]
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 5 :battle-unit)
    (press :cross)))
  (println "Action ended.")
  )

(defn confine-unit [target n]
  (println "Confining.")
  (select-active)
  (play-input
   (concat
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 3 :battle-unit)
    (press :cross)))

  (select-unit target)
  
  (if (can-confine?)
    (do
      (play-input
       (concat
        (press :cross)
        (menu-key-seq (battle-confine-cursor) n :confine)
        (press :cross)
        (cancel))))
    (do
      (cancel)
      (cancel)
      (move-unit target 20 :away))))

(defn special-stage []
  (println "At special stage.")
  (play-input
   (concat
    (press :cross 20)))
  (wait-until-active))

(defn start-stage []
  (println "Stage started.")
  (wait-until-active))

(defn finish-stage []
  (println "Finished stage.")
  (play-input
   (concat
    ;; skip bol increment
    (press :cross)
    ;; close result menu
    (press :cross)
    ;; skip title status in dungeons
    ;; TODO: detect if in dungeon
    (press :cross)
    [(wait 40)]
    (wait-until-active))))
