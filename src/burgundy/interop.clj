(ns burgundy.interop
  (:import com.ruin.psp.PSP)
  (:import java.io.File))

(def object-start-offset 0x01491070)
(def object-size 2136)

(def object-stat-offset 793) ;; 5? stats, u32 LE
(def object-stat-offset-modified 817) ;; accounts for equipment.

(def object-coord-offset 0x84)

(def object-max 32)

;; (def object-spec (spec :unk-a (bytes-type 116)
;;                        :x (int32-type)
;;                        :y (int32-type)
;;                        :z (int32-type)
;;                        :unk-b (bytes-type 600)
;;                        :name (string-type 16)))

;; (def-typed-struct unit-struct
;;   :unk-a (array int8 116)
;;   :x float32-le
;;   :y float32-le
;;   :z float32-le
;;   :unk-b (array int8 600)
;;   :name (array int8 16)
;;   :unk-c (array int8 1380)
;;   )

(defn contiguous-memory
  "Returns count wrapped buffers of size bytes starting at offset."
  [offset size count]
  (let [mem (PSP/readRam offset (* count size))
        objs (partition size mem)]
    (->> objs
         (map byte-array)
         (map bytes))))

;; (defn apply-spec
;;   "Creates a struct from the wrapped buffer buf using spec."
;;   [spec buf]
;;   (compose-buffer spec :orig-buffer buf))

;; (defn units []
;;   (let [objs (contiguous-memory object-start-offset object-size object-max)]
;;     (wrap unit-struct objs)))

(defn units []
  (let [objs (contiguous-memory object-start-offset object-size object-max)]
    objs))
