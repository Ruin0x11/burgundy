(ns burgundy.menu
  (:require [burgundy.interop :refer :all])
  )

(def menu-scroll-amounts
  {:attack 8
   :confine 5
   :status 7})

(def menu-delay 6)

(defn menu-key-seq
  "Calculates the optimal input sequence to traverse a menu from position start to position end."
  ([start end] (let [diff (Math/abs (- start end))]
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


(defn move-unit [x z]
  (play-input
   (concat
    [(wait menu-delay)
     [[:cross] 1]
     (wait menu-delay)]
    (menu-key-seq (battle-unit-cursor) 0)
    [[[:cross] menu-delay]
     (wait menu-delay)
     [[:analog 0.0 1.0] 20]
     (wait menu-delay)
     [[:cross] menu-delay]
     ;; TODO: gather info about when out of control
     (wait 40)])))

(defn end-action []
  (play-input
   (concat
    [(wait menu-delay)
     [[:cross] 1]
     (wait menu-delay)]
    (menu-key-seq (battle-unit-cursor) 5)
    [[[:cross] 1]
     (wait menu-delay)])))

(defn confine-unit [n]
  (play-input
   (concat
    [[[:cross] menu-delay]]
    (menu-key-seq (battle-unit-cursor) 3)
    [[[:cross] menu-delay]]
    (menu-key-seq (battle-confine-cursor) n :confine)
    [[[:cross] menu-delay]])))
