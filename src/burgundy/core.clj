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

(def addresses [0x001B20D0 0x001B20D4 0x001B20D8 0x001B20DB])

(defn shutdown! []
  (PSP/shutdown))

(defn step []
  (print "step")
  (PSP/step))

(defn restart! []
  (PSP/startEmulator (.getCanonicalPath phantom-brave-us))
  (PSP/loadSaveState 0))

(defn play [n]
  (dorun (dotimes [_ n]
           (Thread/sleep 1)

           (doseq [i addresses]
             (printf "RAM at %x: %08X\n" i (PSP/readRAMU32 i)))
           (println)
           (PSP/nstep 21))))

(defn continue! []
  (println "continue")
  (play Integer/MAX_VALUE))

(defn -main
  [& args]
  (restart!)
  (continue!))
