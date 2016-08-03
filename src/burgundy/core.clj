(ns burgundy.core
  (:import com.ruin.psp.PSP)
  (:import java.io.File)
  (:gen-class))

(use 'clojure.reflect)

(defn all-methods [x]
    (->> x reflect
           :members
           (filter :return-type)
           (map :name)
           sort
           (map #(str "." %) )
           distinct
           println))

(all-methods PSP)

(def user-home (File. (System/getProperty "user.home")))

(clojure.lang.RT/loadLibrary "psp")
;; (PSP/loadPspLibrary)

;; (PSP/greetSelf)

(def phantom-brave-jp
  (File. user-home "game/phantom-brave-jp.iso"))
(def phantom-brave-us
  (File. user-home "game/phantom-brave-us.iso"))

(def addresses [0x01454960 0x01454E1C 0x014975A0 0x01497A5C 0x0012E8A4 0x0012E89C 0x0012E8A0])

(defn shutdown! []
  (PSP/shutdown))

(defn step []
  (print "step")
  (PSP/step))

(defn restart! []
  (PSP/startEmulator (.getCanonicalPath phantom-brave-us))
  (PSP/loadSaveState 2))

(defn doIt []
  (let [arr (byte-array (PSP/readRam 0x01497fc0 2136))
        thing (apply str (map char (take 16 arr)))]
    (println thing)))

(defn play [n]
  (dorun (dotimes [_ n]
           (Thread/sleep 1)

           (doseq [i addresses]
             (printf "RAM at %x: %08X %d %.6f\n" i (PSP/readRAMU32 i) (PSP/readRAMU16 i) (PSP/readRAMU32Float i)))
           (doIt)
           (println)
           (PSP/nstep 21))))

(defn continue! []
  (println "continue")
  (play Integer/MAX_VALUE))

(defn -main
  [& args]
  (restart!)
  (continue!)
  (shutdown!))
