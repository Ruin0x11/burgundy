(ns burgundy.isle
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.skill :refer :all]
            [burgundy.dungeon :refer :all]
            [burgundy.charagen :refer :all]
            [burgundy.types :refer :all]
            [burgundy.fusion :refer :all]
            [burgundy.menu :refer :all]))

(def island-dist 7.0)

(def ghost-pos [194 193])

(defn goto-marona []
  (move-to-unit (unit-by-name "Marona") :island))

(defn pick-up [target]
  (move-to-unit target :island)
  (play-input [□]))

(defn throw-to [target]
  (play-input [□])
  (move-to-unit target)
  (find-actionable (unit-by-name "Ash") can-throw?)
  ;; TODO: detect if active
  (play-input [□ [:wait 40]]))

(defn throw-away [pos]
  (let [[x z] pos]
    (when (holding?)
      (play-input [□])
      (move-to-point x z)
      (find-actionable (unit-by-name "Ash") can-throw?)
      (play-input [□ [:wait 40]]))))

(defn stop-marona []
  (pick-up (unit-by-name "Mysterious Ghost"))
  (throw-to (unit-by-name "Marona"))
  (pick-up (unit-by-name "Marona"))
  (throw-away ghost-pos))

(defn heal-party []
  (move-to-unit (unit-by-name "Europa") :island)
  (play-input [× [:wait 30] × [:wait 15] [:start] ○]))

(defn create-dungeon [options]
  (when (< (count (dungeons)) max-dungeons)
    (move-to-unit (unit-by-name "Dulap") :island)
    (play-input [× [:wait 50] × [:wait 15] ×])
    (search-for-dungeon options)
    (wait 10)
    (cancel)))

(defn delete-dungeon [pos]
  (when (> (count (dungeons)) 0)
    (move-to-unit (unit-by-name "Dulap") :island)
    (play-input [× [:wait 50] × [:wait 15] × ↓ ×
                 (menu-key-seq (dungeon-menu-cursor)
                               pos
                               :dungeon-menu
                               (count (dungeons)))
                 [:wait 4] × ○ ○])))

(defn go-to-dungeon [pos]
  (when (> (count (dungeons)) 0)
    (move-to-unit (unit-by-name "Dulap") :island)
    (play-input [× [:wait 50] × [:wait 15] × ↓ ↓ ×
                 (menu-key-seq (dungeon-menu-cursor)
                               pos
                               :dungeon-menu
                               (count (dungeons)))
                 [:wait 4] ×])))

(defn summon [pos]
  (goto-marona)
  (play-input [× [:wait 30] × [:wait 10]
               (menu-key-seq (marona-menu-cursor) pos :marona 99)
               × [:wait 30]]))

(defn store [pos]
  (goto-marona)
  (play-input [× [:wait 30] ↓ × [:wait 10]
               (menu-key-seq (marona-menu-cursor) pos :marona 99)
               × [:wait 30]]))

(defn create-character
  ([class exp] (create-character class exp 2))
  ([class exp attempts]
   (when-not (= attempts 0)
     (when (in? (vals class-type-kws) class)
       (goto-marona)
       (play-input [× [:wait 30] ↓ ↓ × [:wait 10]])
       (let [pos (soul-with-class class)
             soul-count (count (neutral-units))]
         (if (= pos -1)
           (do
             (cancel)
             (recur class exp (- attempts 1)))
           (do
             (play-input [(menu-key-seq 0 pos :charagen soul-count)
                          (make-key-seq ↑ (- exp 1))
                          × [:start] × ↑ × [:wait 80]])
             true)))))))

(defn banish [unit]
  (pick-up unit)
  (goto-marona)
  (play-input [× [:wait 20] × [:wait 20] × [:wait 100]]))

(defn fuse [target material]
  (move-to-unit (unit-by-name "Miyu") :island)
  (play-input [× [:wait 50] × [:wait 10]])
  (select-fusion-units target material)
  (play-input [↑ × × [:wait 120] ○]))
