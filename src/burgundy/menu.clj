(ns burgundy.menu
  (:require [burgundy.interop :refer :all]
            [burgundy.skill :refer :all]
            [burgundy.unit :refer :all]))

(def menu-key-types
  {:default [:down :rtrigger :up :ltrigger]
   :charagen [:left :left :right :right]})

(def menu-scroll-amounts
  {:attack 8
   :confine 5
   :status 7})

(def menu-sizes
  {:battle-unit 6 ;; plus one if marona
   :battle-main 4
   :marona 5})

(def menu-battle-unit
  {:move 0
   :attack 1
   :throw 2
   :confine 3
   :status 4
   :end-action 5})

(defn adjust-menu-size [size menu-type]
  (if (and (not (on-island?))
           (not (is-marona?))
           (= menu-type :battle-unit))
    (- size 1)
    size))

(defn adjust-menu-pos [pos menu-type]
  (if (and (not (on-island?))
           (not (is-marona?))
           (= menu-type :battle-unit)
           (> pos 3))
    (- pos 1)
    pos))

(defn get-menu-buttons [start end size diff menu-type]
  (let [menu-type (if (contains? menu-key-types menu-type) menu-type :default)
        buttons (menu-type menu-key-types)]
    (if (>= (mod diff size) (/ size 2))
      (if (> start end)
        (take 2 buttons)
        (drop 2 buttons))
      (if (> start end)
        (drop 2 buttons)
        (take 2 buttons)))))

(defn make-key-seq [key amount]
  [:seq (vec (repeat amount key))])

(defn menu-key-seq
  "Calculates an input sequence to traverse a menu from position start to position end."
  ([start end menu-type]
   (let [size (if (contains? menu-sizes menu-type)
                (menu-type menu-sizes) 1)]
     (menu-key-seq start end menu-type size)))
  ([start end menu-type size]
   (let [size (adjust-menu-size size menu-type)
         start (adjust-menu-pos start menu-type)
         end (adjust-menu-pos end menu-type)
         diff (Math/abs (- end start))
         amount (if (> diff (Math/floor (/ size 2)))
                  (Math/abs (- diff size))
                  diff)
         [arrow trigger] (get-menu-buttons start end size diff menu-type)]
     (make-key-seq [arrow] amount)
     )))


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
  (play-input [○]))

(defn select-active
  "Try to select the active unit."
  []
  (if (> (dist-unit (active-unit)) selection-dist)
    ;; canceling also cancels previous moves, so only cancel if I haven't moved.
    (if (has-moved?)
      (move-to (active-unit))
      (cancel)))
  (select-unit-in-cursor (active-unit)))

(defn find-actionable
  "Moves the cursor towards a unit until a condition is satisfied."
  ([unit] (find-actionable unit can-move?))
  ([unit condition]
   (if (> (dist-unit unit) 1.0)
     (when-not (condition)
       (move-towards unit)
       (recur unit condition))
     (do
       (cancel)
       (cancel)))))

(defn move-unit [target dist & [dir]]
  (let [angle 90
        [ax ay] (angle->analog angle 1.0)]
    (select-active)
    (play-input
     [×
      (menu-key-seq (battle-unit-cursor) 0 :battle-unit)
      ×])

    (move-to target 10.0 dir)

    (find-actionable (active-unit))
    (play-input [[:cross 20]])
    (wait-until-active)))

(defn move-unit-quick
  [unit target dist & [dir]]
  (move-to target dist dir)

  (play-input
   [[:wait 20]
    [:cross 20]
    (menu-key-seq (battle-unit-cursor) 0 :battle-unit)])

  (find-actionable (active-unit))
  (play-input [[:cross 20]])
  (wait-until-active))

(defn skill-pos-for-target [target skills]
  (let [unit (active-unit)]
    (if (empty? skills)
      0
      (apply min-key skill-sp-cost skills))))

(defn select-skill [skill skills]
  (play-input
   [[:wait 10] ×
    (menu-key-seq (battle-unit-cursor) 1 :battle-unit)
    ×])

  (play-input
   [(menu-key-seq (battle-attack-cursor)
                  (get-skill-pos skill skills)
                  :battle-attack
                  (count skills))
    [:cross 4]]))

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

   (select-skill skill skills)

   (wait 20)
   (if (can-attack?)
     (do
       (play-input [×])
       (wait-until-active)
       (if (and (not (has-attacked?)) (> retries 0))
         (recur target skill skills (- retries 1))))
     (do
       (cancel)
       (cancel)
       (cancel)))))

(defn return []
  (let [skills (get-all-skills)]
    (if (has-skill? :return)
      (do
        (select-skill (:return skill-type-ids) skills)
        (play-input [↑ ×])
        (wait 50))
      (println "Um, I don't have Return. This is kinda bad..."))))

(defn end-action []
  (select-active)
  (play-input
   [[:wait 4] ×
    (menu-key-seq (battle-unit-cursor) 5 :battle-unit)
    × [:wait 20]])
  (wait-until-active)
  )

(defn confine-unit [target n]
  (select-active)
  (play-input
   [[:wait 4] ×
    (menu-key-seq (battle-unit-cursor) 3 :battle-unit)
    ×])

  (select-unit target)
  
  (if (can-confine?)
    (do
      (play-input
       [×
        (menu-key-seq (battle-confine-cursor) n :confine)
        × ○
        [:wait 10]]))
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
   [× × [:cross 40]
    (wait-until-active)]))

