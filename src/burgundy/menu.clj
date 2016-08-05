(ns burgundy.menu
  (:require [burgundy.interop :refer :all])
  )

(def menu-scroll-amounts
  {:attack 8
   :confine 5
   :status 7})

(def menu-sizes
  {:attack 999
   :battle-unit 6
   :battle-main 4
   :confine 999
   :status 999})

(def menu-delay 6)

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

(defn cancel []
  (println "cancel")
  (play-input
   [(wait 20)
    [[:circle] 1]
    [[] 20]]))

(defn move-unit [unit]
  (println "mov")
  (let [angle 90
        [ax ay] (angle->analog angle 1.0)]
    (play-input
     (concat
      [(wait 40)
       [[:cross] 1]
       (wait menu-delay)]
      (menu-key-seq (battle-unit-cursor) 0)
      [[[:cross] menu-delay]
       (wait menu-delay)]))

    (get-closer unit 10.0)

    (play-input
     (concat
      [(wait menu-delay)
       [[:cross] menu-delay]
       ;; TODO: gather info about when out of control
       (wait 40)]))))

(defn move-unit-quick [unit]
  (get-closer unit 10.0)
  (play-input
   (concat
    [(wait 10)
     [[:cross] 1]
     (wait menu-delay)]
    (menu-key-seq (battle-unit-cursor) 0)
    [[[:cross] menu-delay]
     (wait 40)]))
  )

(defn attack [unit]
  (println "Take this.")
  (println (str "pos:" (battle-attack-cursor)))
  (get-closer unit)
  (play-input
   (concat
    [(wait 10)
     [[:cross] 1]
     (wait menu-delay)]
    (menu-key-seq (battle-unit-cursor) 1)
    [[[:cross] 1]
     (wait 20)]))

  (play-input
   (concat
    (menu-key-seq (battle-attack-cursor) 0)
     [[[:cross] 1]
     (wait menu-delay)
     [[:cross] 1]
     (wait 80)])))


(defn end-action []
  (play-input
   (concat
    [(wait 40)
     [[:cross] 1]
     (wait menu-delay)]
    (menu-key-seq (battle-unit-cursor) 5)
    [[[:cross] 1]])))

(defn confine-unit [n]
  (play-input
   (concat
    [[[:cross] menu-delay]]
    (menu-key-seq (battle-unit-cursor) 3)
    [[[:cross] menu-delay]]
    (menu-key-seq (battle-confine-cursor) n :confine)
    [[[:cross] menu-delay]])))
