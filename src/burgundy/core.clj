(ns burgundy.core
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]
            [burgundy.repl :refer :all])
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

(def run-repl? true)

;; (def addresses [0x01454960 0x01454E1C 0x014975A0 0x01497A5C 0x0012E8A4 0x0012E89C 0x0012E8A0])

(defn print-object [obj]
  (println (str
            ;; (:name obj) "\n"
            (seq (:name obj))
            )))

(defn snoop [addr]
  (doseq [i addr]
    (printf "RAM at %x: %08X %d %.6f\n" i (PSP/readRAMU32 i) (PSP/readRAMU16 i) (PSP/readRAMU32Float i)))
  (println))

(defn snoop-range [start byte-offset count]
  (snoop (take count (range start (+ (* byte-offset count) start) byte-offset))))

(defn confine-state []
  (load-state "confine"))

(defn restart! []
  (PSP/startEmulator (.getCanonicalPath phantom-brave-us)))

(defn play [n]
  (dorun (dotimes [_ n]
           (Thread/sleep 1)
           ;; (println (unit-name (closest (first (my-units)) (enemy-units))))
           ;; (println (get-pos (first (my-units))))
           ;; (println (pos-z (first (my-units))))
           ;; (println (dist (first (my-units))))
           ;; (println (get-player-pos))
           ;; (get-closer (first (my-units)))

           (cancel)
           (let [main (first (my-units))
                 target (closest main (enemy-units))]
             (cond
               (in-range? main target 25)
               (do
                 (println (dist main target))
                 (println (get-pos main))
                 (println (get-pos target))
                 (attack target))
               :else
               (do
                 (println (dist main target))
                 (println (get-pos main))
                 (println (get-pos target))
                 (move-unit-quick (closest (first (my-units)) (enemy-units)))
                 (when (in-range? (first (my-units)) target 25)
                   (attack target)))))
           (end-action)

           (step)

           (println (PSP/getBattleAttackMenuCursorPos))
           )))

(defn continue! []
  (println "continue")
  (play Integer/MAX_VALUE))

(defn -main
  [& args]
  (let [api (com.ruin.psp.PSP.)]
    (bind-api! api)

    (when run-repl?
      (repl-control! true)
      (start-repl! 7777))

    (restart!)
    (PSP/setFramelimit false)
    (load-state "confine")
    (step)
    (step)
    ;; (confine-unit 6)
    (continue!)
    (shutdown!)))
