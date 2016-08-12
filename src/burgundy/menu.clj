(ns burgundy.menu
  (:require [burgundy.interop :refer :all]
            [burgundy.skill :refer :all]
            [burgundy.unit :refer :all]))

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
     [:seq (vec (repeat amount [arrow]))])))


(defn wait-until-active
  ([]
   (println "---Waiting.---")
   (wait-until-active 0))
  ([elapsed-frames]
   (if-not (can-enter-input?)
     (do
       (step)
       (recur (+ elapsed-frames 1)))
     (do
       (step)
       (println "===Active.===")))))

(defn cancel []
  (play-input ○))

(defn select-active
  "Try to select the active unit."
  []
  (if (> (dist-unit (active-unit)) selection-dist)
    ;; canceling also cancels previous moves, so only cancel if I haven't moved.
    (if (has-moved?)
      (move-to (active-unit))
      (cancel)))
  (select-unit-in-cursor (active-unit)))

(defn look-for-walkable
  "Moves the cursor towards the unit until it finds a piece of terrain it can walk towards, then moves there.

  Only to be called at the move menu."
  [unit]
  (if (> (dist-unit unit) 1.0)
    (if (can-move?)
      (do
        (play-input [:cross])
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
  (let [angle 90
        [ax ay] (angle->analog angle 1.0)]
    (select-active)
    (play-input
     [:cross]
     (menu-key-seq (battle-unit-cursor) 0 :battle-unit)
     [:cross])

    (move-to target 10.0 dir)

    (if (can-move?)
      (do
        (play-input [:cross 20])
        (wait-until-active))
      ;; TODO: fix.
      (look-for-walkable (active-unit)))))

(defn move-unit-quick
  [unit target dist & [dir]]
  (move-to target dist dir)

  (play-input
   (concat
    [:wait 20]
    [:cross 20]
    (menu-key-seq (battle-unit-cursor) 0 :battle-unit)))

  (if (can-move?)
    (do
      (play-input [:cross 20])
      (wait-until-active))
    (look-for-walkable (active-unit))))

(defn select-skill [target skills]
  (let [unit (active-unit)]
    (if (empty? skills)
      0
      (apply min-key skill-sp-cost skills))))

(defn attack
  ([target skill skills] (attack target skill skills 3))
  ([target skill skills retries]
   (println "Take this.")
   (println (str "pos:" (battle-attack-cursor)))

   (println (skill-name skill))
   (if (is-single-target? skill)
     (do (println "single target")
         (select-unit target))
     (do
       (println "GO")
       (move-unit target (skill-range skill))
       (select-unit target)))

   (play-input
    [:wait 10]
    [:cross]
    (menu-key-seq (battle-unit-cursor) 1 :battle-unit)
    [:cross])

   (play-input
    [(menu-key-seq (battle-attack-cursor)
                   (get-skill-pos skill skills)
                   :battle-attack
                   (count skills))
     [:cross 4]])

   (wait 20)
   (if (can-attack?)
     (do
       (play-input [:cross])
       (wait-until-active)
       (if (and (not (has-attacked?)) (> retries 0))
         (recur target skill skills (- retries 1))))
     (do
       (cancel)
       (cancel)
       (cancel)))))

(defn end-action []
  (select-active)
  (play-input
   [[:wait 4]
    [:cross]
    (menu-key-seq (battle-unit-cursor) 5 :battle-unit)
    [:cross]
    [:wait 20]])
  (wait-until-active)
  )

(defn confine-unit [target n]
  (select-active)
  (play-input
   [[:wait 4]
    [:cross]
    (menu-key-seq (battle-unit-cursor) 3 :battle-unit)
    [:cross]])

  (select-unit target)
  
  (if (can-confine?)
    (do
      (play-input
       [:cross]
       (menu-key-seq (battle-confine-cursor) n :confine)
       [:cross]
       (cancel)
       [:wait 10]))
    (do
      (cancel)
      (cancel))))

(defn intrusion-stage []
  (play-input
   [[:cross 20]]))

(defn start-stage []
  (wait-until-active))

(defn finish-stage []
  (play-input
   [:cross]
   [:cross]
   [:cross 40]
   (wait-until-active)))
