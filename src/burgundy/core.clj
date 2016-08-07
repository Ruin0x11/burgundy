(ns burgundy.core
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]
            [burgundy.battle :refer :all]
            [burgundy.repl :refer :all])
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

;; (def addresses [0x01454960 0x01454E1C 0x014975A0 0x01497A5C 0x0012E8A4 0x0012E89C 0x0012E8A0])

(def run-ai? (atom false))

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

(defn run-once []
  (save-state "temp")
  (step)
  (run-battle-engine))

(defn rewind []
  (load-state "temp")
  (step)
  (run-battle-engine))

(defn rewind-stop []
  (load-state "temp"))

(defn play [n]
  (dorun (dotimes [_ n]
           (Thread/sleep 1)
           ;; (list-units)
           ;; (snoop-range 0x01458b20 4 64)
           (dump (active-unit))
           (println (has-attacked?))
           (when @run-ai?
             (run-battle-engine))
           (step))))

(defn continue! []
  (println "continue")
  (play Integer/MAX_VALUE))


(defn -main
  [& args]
  (let [api (com.ruin.psp.PSP.)]
    (bind-api! api)

    (println (str "AI: " (if @run-ai? "ENABLED" "DISABLED")))

    (when run-repl?
      (repl-control! true)
      (start-repl! 7777))

    (restart!)
    (PSP/setFramelimit false)
    (load-state "go")
    (step)
    (step)
    (continue!)
    (shutdown!)))
