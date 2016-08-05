(ns burgundy.interop
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

(defn island-menu-cursor [] (PSP/getIslandMenuCursorPos))

(defn status-menu-cursor [] (PSP/getStatusMenuCursorPos))

(defn battle-menu-cursor [] (PSP/getBattleMenuCursorPos))

(defn battle-unit-cursor [] (PSP/getBattleUnitMenuCursorPos))

(defn battle-confine-cursor [] (PSP/getConfineMenuCursorPos))

(def scroll-amounts
  {:attack 8
   :confine 5
   :status 7})

(defn menu-key-seq
  "Calculates the optimal input sequence to traverse a menu from position start to position end."
  ([start end] (- start end))
  ([start end menu-type]
   (let [scroll-amt (get scroll-amounts menu-type)
         diff (Math/abs (- start end))]
     (+ (/ diff scroll-amt) (mod diff scroll-amt)))))

(defn contiguous-memory
  "Returns count wrapped buffers of size bytes starting at offset."
  [offset size count]
  (let [mem (PSP/readRam offset (* count size))
        objs (partition size mem)]
    (->> objs
         (map byte-array)
         (map bytes))))

;; (defn units [] (PSP/getFriendlyUnits)) 

(def button-masks
  {:square    0x8000
   :triangle  0x1000
   :circle    0x2000
   :cross     0x4000
   :up        0x0010
   :down      0x0040
   :left      0x0080
   :right     0x0020
   :start     0x0008
   :select    0x0001
   :ltrigger  0x0100
   :rtrigger  0x0200})

(defn button-bits
  "Converts a sequence of button keywords into a button bitmask."
  [buttons]
  (reduce #(bit-or %1 (%2 button-masks)) 0x0000 buttons))

(defn play-input
  "Sends input commands.
  Expects a vector of pairs of a vector of button keywords and the number of frames to hold it for."
  [input]
  (doseq [[buttons frames] input]
      (dotimes [i frames]
        (let [bitmask (button-bits buttons)]
          (printf "%x\n" bitmask)
          (println i)
          (PSP/nstep bitmask)))))

(defn test-input []
  (println "asd")
  (play-input [[[:up]    20]
               [[:down]  20]
               [[:left]  20]
               [[:right] 20]]))
