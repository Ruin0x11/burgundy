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

(defn snoop-range
  ([start] (snoop-range start 4 8))
  ([start byte-offset count]
   (let [addresses (take count (range start (+ (* byte-offset count) start) byte-offset))]
     (doseq-indexed i [addr addresses]
                    (printf "0x%8X (+%04X): %08X %d %.6f\n" addr (* byte-offset i) (PSP/readRAMU32 addr) (PSP/readRAMU16 addr) (PSP/readRAMU32Float addr)))
     (println))))

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
           
           ;; (snoop-range 0x01546300 4 64)
           ;; (snoop-range (unit-offset (active-unit)) 4 64)
           (if @run-ai?
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
    (load-state "trolly")
    (step)
    (step)
    (gen-type-kw-maps)
    (continue!)
    (shutdown!)))
