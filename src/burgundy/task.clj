(ns burgundy.task
  )

(defmacro def-task
  [name args desc test action on-failure]
  `(defn ~name ~args
     (let [full-name# (str 'name)
           full-desc# (str ~@desc)]
       {:name full-name#
        :desc full-desc#
        :test (fn [] (do ~test))
        :action (fn [] (do ~@action))
        :on-failure (fn [] (do ~@on-failure))
        })))

(defn run-task
  [task]
  (let [{:keys [name desc action test on-failure]} task]
    (println (str "TASK: " name))
    (println desc)
    (when-not (test)
      (action)
      (when-not (test)
        (on-failure)))))


(def-task level-item [id lvl amt]
               ["Levels item " id " to level " lvl]
               (< lvl 5)
               (+ lvl amt)
               (println "nope"))


(macroexpand '(level-item 1 7 1))

(defmacro asd [id]
  `(defn asd-asd []
     (println (str ~id))))
((asd 3))
