(ns burgundy.core
  (:require [burgundy.interop :refer :all]
            [burgundy.menu :refer :all]
            [burgundy.battle :refer :all]
            [burgundy.queue :refer [cmd]]
            [burgundy.logic :refer [add-team-members]]
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
     (clojure.string/join (map-indexed (fn [i addr]
                                         (format "0x%08X (+%04X): %08X %d %.6f\n" addr (* byte-offset i)
                                                 (PSP/readRAMU32 addr) (PSP/readRAMU16 addr) (PSP/readRAMU32Float addr))) addresses))
     )))

(defn snoop-bytes
  ([start] (snoop-bytes start 16))
  ([start count] (let [addresses (take count (range start (+ (* 1 count) start) 1))]
                   (clojure.string/join (map-indexed (fn [i addr]
                                                       (format "0x%08X (+%04X): %02X %d\n" addr (* 1 i)
                                                               (PSP/readRAMU8 addr) (PSP/readRAMU8 addr))) addresses)))))

(defn snoop
  ([start] (snoop start 4 8))
  ([start byte-offset count & [kind]]
   (let [func
         (case kind
           :bytes #(snoop-bytes start count)
           #(snoop-range start byte-offset count))]
     (repeatedly #(println (cmd (func)))))))

(defn run-once []
  (save-state "temp")
  (step)
  (run-battle-engine))

(defn rewind-run []
  (load-state "temp")
  (step)
  (run-battle-engine))

(defn rewind []
  (load-state "temp"))

(defn play [n]
  (load-skill-types)
  (gen-type-kw-maps)
  (add-team-members)
  (dorun (dotimes [_ n]
           (Thread/sleep 1)
           ;; (list-units)

           ;; (snoop-range 0x01546300 4 64)
           ;; (snoop-range (unit-offset (active-unit)) 4 64)
           ;; (if @run-ai?
           ;;   )
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
    (load-state "55")
    (step)
    (step)
    (continue!)
    (shutdown!)))
