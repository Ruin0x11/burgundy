(ns burgundy.core
  (:import com.ruin.psp.PSP)
  (:gen-class))

(clojure.lang.RT/loadLibrary "psp")
;; (PSP/loadPspLibrary)

(PSP/greetSelf)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

