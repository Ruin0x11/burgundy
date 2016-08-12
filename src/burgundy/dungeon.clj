(ns burgundy.dungeon
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]))

(def dungeon-enemy-types
  {0x01 :male
   0x02 :female
   0x03 :feeble
   0x04 :puny
   0x05 :warrior
   0x06 :mage
   0x07 :artisan
   0x08 :beast
   0x09 :giant
   0x0A :creepy
   0x0B :horror
   0x0C :owl
   0x0D :demiman
   0x0E :chibi
   0x0F :putty
   0x10 :slime
   0x11 :common
   0x12 :shroom
   0x13 :weird
   0x33 :varied})

(def dungeon-item-types
  {0x97 :knife
   0x98 :sword
   0x99 :axe
   0x9a :big-axe
   0x9b :spear
   0x9c :mace
   0x9d :staff
   0x9e :book
   0xab :cutlery
   0xac :magical
   0x65 :tree
   0x66 :wilted
   0x67 :rock
   0x68 :plant
   0x69 :grass
   0x6a :flower
   0x6b :cactus
   0x6c :garden
   0x6d :box
   0x6e :fish
   0x6f :bone
   0x70 :tool
   0x71 :food
   0x79 :item})

(def dungeon-sizes
  {:small    [0x00 0x0A]
   :standard [0x0B 0x0E]
   :large    [0x1F 0xFF]})

(def dungeon-enemy-amounts
  {:few    [0x00 0x04]
   :normal [0x05 0x09]
   :many   [0x0A 0xFF]})

(def dungeon-ground-types
  {0x00 :normal
   0x01 :smooth
   0x02 :bouncy
   0x03 :varies})

(def dungeon-prohibition-types
  {0x00 :normal
   0x01 :item
   0x02 :item-variable})

(defn dungeon-enemy-type [dungeon]
  (get dungeon-enemy-types (.getEnemyType dungeon)))

(defn dungeon-item-type [dungeon]
  (get dungeon-item-types (.getEnemyType dungeon)))

(defn dungeon-info [dungeon]
  {:level        (.getLevel dungeon)
   :floors       (.getMaxFloors dungeon)
   :ground-type  (get dungeon-ground-types (.getGroundType dungeon))
   :enemy-type   (get dungeon-enemy-types (.getEnemyType dungeon))
   :item-type    (get dungeon-item-types (.getItemType dungeon))
   :enemy-amount (.getEnemyAmount dungeon)
   :item-amount  (.getItemAmount dungeon)
   :prohibition  (get dungeon-prohibition-types (.getProhibition dungeon))
   :exp          (.getExp dungeon)
   :bonus        (.getBonus dungeon)
   })

(def base-dungeon
  {:level [0 9999] :floors [0 99] :ground-type :normal :prohibition :normal})

(def perfect-title-dungeon
  (merge base-dungeon
         {:exp 11
          :bonus 11}))

(def leveling-dungeon
  (merge base-dungeon
         {:level [7000 9999]
          :enemy-type :beast
          :enemy-amount (:many dungeon-enemy-amounts)}))

(defn check-prop [val-or-range result]
  (println val-or-range result)
  (if (vector? val-or-range)
    (let [[min max] val-or-range]
      (<= min result max))
    (= val-or-range result)))

(defn create-dungeon [options-map]
  (let [options (merge base-dungeon options-map)
        gen (dungeon-info (generated-dungeon))
        ;; only compare the keys that are found in the options
        considering (select-keys gen (keys options))
        found? (every? true? (map check-prop (vals options) (vals considering)))]
    (println (vals options)(vals considering) found?)
    (println)
    (if-not found?
      (do
        (println "next")
        (play-input [Ｒ])
        (recur options-map))
      (play-input [×]))))
