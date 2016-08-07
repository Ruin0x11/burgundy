(ns burgundy.task
  (:use clojure.data.priority-map))

(def battle-tasks (atom (priority-map)))

(defn add-task
  "Adds the provided task to the priority queue."
  [task queue]
    (do (swap! queue conj [task (:priority task)])))

(defmacro def-task
  "Creates a task.

  :priority - task priority, with lower values deemed higher priority
  :action - function to run when executing the test
  :test - function to determine if the desired state is achieved. the action is not run if this is true
  :on-failure - function to run if the function provided by :test returns false"
  [task-name args & {:keys [desc priority test action on-failure]}]
  `(defn ~task-name ~args
     (let [full-name# (name '~task-name)
           full-desc# (str ~@desc)]
       {:name full-name#
        :desc full-desc#
        :priority ~priority
        :test (fn [] (do ~test))
        :action (fn [] (do ~@action))
        :on-failure (fn [] (do ~@on-failure))
        })))

(defn run-task
  [task]
  (let [{:keys [name desc priority action test on-failure]} task]
    (println (str "TASK: " name))
    (println desc)
    (if (test)
      (do
        (println (str name ": test passed"))
        (action))
      (do
        (println (str name ": test failed"))
        (when-not (or (test)
                      (nil? on-failure))
          (on-failure))))))

(def-task level-item [id lvl amt]
  :desc ["Levels item " id " to level " lvl]
  :priority 0
  :test (< lvl 5)
  :action ((+ lvl amt))
  :on-failure (println "nope"))


(def-task nothing-task []
  :action ((println "go"))
  )
