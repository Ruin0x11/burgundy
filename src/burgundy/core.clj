(ns burgundy.core
  (:require ;[vertigo.core :refer [get-in]]
   [burgundy.interop :refer :all])
  (:import com.ruin.psp.PSP)
  (:import java.io.File)

  (:gen-class))

(clojure.lang.RT/loadLibrary "psp")

(def user-home (File. (System/getProperty "user.home")))

(def phantom-brave-jp
  (File. user-home "game/phantom-brave-jp.iso"))
(def phantom-brave-us
  (File. user-home "game/phantom-brave-us.iso"))

(def addresses [0x01454960 0x01454E1C 0x014975A0 0x01497A5C 0x0012E8A4 0x0012E89C 0x0012E8A0])

(defn shutdown! []
  (PSP/shutdown))

(defn step []
  (PSP/step))

(defn restart! []
  (PSP/startEmulator (.getCanonicalPath phantom-brave-us))
  (PSP/loadSaveState 2))

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

(defn play [n]
  (dorun (dotimes [_ n]
           (Thread/sleep 1)
           ;; 0x01499ecc
           (snoop-range (+ (* 17 object-size) object-start-offset 100) 8 64)
           (println)
           (PSP/nstep 21))))

(defn continue! []
  (println "continue")
  (play Integer/MAX_VALUE))

(defn -main
  [& args]
  (restart!)
  (step)
  (step)
  (continue!)
  (shutdown!))
