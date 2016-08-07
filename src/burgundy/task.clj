(ns burgundy.task
  (:use clojure.data.priority-map))

(def battle-tasks (atom (priority-map)))

(defn add-task
  "Adds the provided task to the given priority queue."
  ;;TODO: only temporary
  ([task] (add-task task battle-tasks))
  ([task queue]
   (do (swap! queue conj [task (:priority task)]))))

(defmacro def-task
  "Creates a task.

  :priority - task priority, with lower values deemed higher priority
  :max-attempts - number of times to rerun the task if the goal state isn't reached
  :action - function to run when executing the task
  :goal-state - function to determine if the desired state is achieved. the action is not run if this is true
  :on-failure - function to run if the function provided by :goal-state returns false"
  [task-name args & {:keys [desc priority max-attempts goal-state action on-failure]}]
  `(defn ~task-name ~args
     (let [full-name# (name '~task-name)
           full-desc# (str ~@desc)]
       {:name full-name#
        :desc full-desc#
        :priority ~priority
        :max-attempts ~max-attempts
        :goal-state (fn [] (do ~goal-state))
        :action (fn [] (do ~action))
        :on-failure (fn [] (do ~on-failure))
        })))

(defn run-task
  ([task]
   (println (str "TASK: " (:name task)))
   (println (str "DESC: " (:desc task)))
   (run-task task 0))
  ([task attempts]
   (let [{:keys [name desc priority max-attempts action goal-state on-failure]} task]
     (cond
       (goal-state)              (println (str name ": goal-state reached"))
       (= max-attempts attempts) (do
                                   (println (str name ": goal-state not reached"))
                                   (when (not (nil? on-failure))
                                     (on-failure)))
       :else                      (do
                                    (println (str "Attempt " (+ 1 attempts)))
                                    (action)
                                    (recur task (+ 1 attempts)))))))

(def-task level-item [id lvl amt]
  :desc ["Levels item " id " to level " lvl]
  :priority 0
  :goal-state (< lvl 5)
  :action ((+ lvl amt))
  :on-failure (println "nope"))


(def-task nothing-task []
  :action ((println "go"))
  )
