(ns burgundy.isle
  (:require [burgundy.interop :refer :all]
            [burgundy.unit :refer :all]
            [burgundy.skill :refer :all]))

(def isle-selection-dist 7.0)

(defn goto-marona []
  (move-to (unit-by-name "Marona") 7.0))

(defn pick-up [target]
  (move-to target 7.0)
  (play-input [□]))

(defn throw-to [target]
  (play-input [□])
  (move-to target 5.0)
  ;; TODO: detect if active
  (play-input [□ [:wait 40]]))

(defn stop-marona []
  (pick-up (unit-by-name "Mysterious Ghost"))
  (throw-to (unit-by-name "Marona"))
  (pick-up (unit-by-name "Marona")))

(defn heal-party []
  (move-to (unit-by-name "Europa") 7.0)
  (play-input [× [:wait 30] × [:wait 10] [:start] ○]))
