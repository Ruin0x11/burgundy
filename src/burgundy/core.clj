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

(defn shutdown! []
  (PSP/shutdown))

(defn restart! []
  (PSP/startEmulator ""))
  ;; (PSP/startEmulator (.getCanonicalPath phantom-brave-jp)))

(defn step []
  (PSP/step))

(defn play [n]
  (dorun (dotimes [_ n]
           (Thread/sleep 1)
           (PSP/step))))

(defn continue! []
  (play Integer/MAX_VALUE))

(defn -main
  [& args]
  (restart!)
  (continue!))
