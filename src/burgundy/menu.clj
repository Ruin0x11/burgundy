(ns burgundy.menu
  (:require [burgundy.interop :refer :all])
  )

(def menu-scroll-amounts
  {:attack 8
   :confine 5
   :status 7})

(def menu-sizes
  {:attack 999
   :battle-unit 6 ;; plus one if marona
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

(defn adjust-menu-size [size menu-type]
  (if (and (not true)
           (= menu-type :battle-unit))
    (- size 1)
    size))

(defn adjust-menu-pos [pos menu-type]
  (if (and (not true)
           (= menu-type :battle-unit)
           (> pos 3))
    (- pos 1)
    pos))

(clojure.pprint/pprint (map #(mod (- % 0) 6) (range 0 6)))

(defn get-menu-buttons [start end size diff]
  (if (< (mod diff size) (/ size 2))
    (if (> start end)
      [:up   :ltrigger]
      [:down :rtrigger]
      )
    (if (> start end)
[:down :ltrigger]
      [:up   :rtrigger]
      )))

(defn menu-key-seq
  "Calculates an input sequence to traverse a menu from position start to position end."
  ([start end menu-type] (menu-key-seq start end menu-type
                                       (menu-type menu-sizes)))
  ([start end menu-type size]
   (let [size (adjust-menu-size size menu-type)
         start (adjust-menu-pos start menu-type)
         end (adjust-menu-pos end menu-type)
         diff (Math/abs (- end start))
         amount (if (> diff (Math/floor (/ size 2)))
                  (Math/abs (- diff size))
                  diff)
         [arrow trigger] (get-menu-buttons start end size diff)]
     (apply concat
            (concat (repeat amount (press arrow)))))))


(defn wait-until-active
  []
  (println "---Waiting.---")
  (while (not (can-enter-input?))
    (step))
  (println "===Active.===")
  (do-nothing 10)
  )

(defn cancel []
  (println "cancel")
  (play-input
   (press :circle)))

(defn select-active
  "Try to select the active unit."
  []
  (if (> (dist (active-unit)) selection-dist)
    ;; canceling also cancels previous moves, so only cancel if I haven't moved.
    (if (has-moved?)
      (move-to (active-unit))
      (cancel)))
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
      (look-for-walkable (active-unit)))
    (println "Moving ended.")))

(defn move-unit-quick
  [unit target dist & [dir]]
  (println "Moving quickly.")
  (move-to target dist dir)

  (play-input
   (concat
    (wait 20)
    (press :cross 20)
    (menu-key-seq (battle-unit-cursor) 0 :battle-unit)))

  (if (can-move?)
    (do
      (play-input
       (press :cross 20))
      (wait-until-active))
    (look-for-walkable (active-unit)))
  (println "Moving ended.")
  )

(defn attack [target]
  (println "Take this.")
  (println (str "pos:" (battle-attack-cursor)))
  (select-unit target)
  (play-input
   (concat
    (wait 10)
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 1 :battle-unit)
    (press :cross)))

  (play-input
   (concat
    (menu-key-seq (battle-attack-cursor) 0 )
    (press :cross)))

  (if (can-attack?)
    (do
      (play-input
       (press :cross))
      (wait-until-active)
      (if (not (has-attacked?))
        (recur target)
        (println "Attack ended.")))
    (do
      (cancel)
      (cancel)
      (cancel)
      (move-unit target 20 :away)))
  )

(defn end-action []
  (println "Ending action.")
  (select-active)
  (play-input
   (concat
    (wait 4)
    (press :cross)
    (menu-key-seq (battle-unit-cursor) 5 :battle-unit)
    (press :cross 20)
    (wait 20)))
  (println "Action ended.")
  (wait-until-active)
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
    (press :cross 20))))

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
    (wait 40)
    (wait-until-active))))
