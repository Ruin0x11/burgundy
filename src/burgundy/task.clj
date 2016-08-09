(ns burgundy.task
  (:require [burgundy.interop :refer [doseq-indexed]])
  (:use clojure.data.priority-map))

(def battle-tasks (atom (priority-map)))

(defn add-task
  "Adds the provided task to the given priority queue."
  ;;TODO: only temporary
  ([task] (add-task task battle-tasks))
  ([task queue times] (dotimes [_ times]) (add-task task queue))
  ([task queue]
   (do (swap! queue conj [task (:priority task)]))))

(defmacro def-task
  "Creates a task for the AI to run.

  :priority - task priority, with lower values deemed higher priority
  :max-attempts - number of times to rerun the task if the goal state isn't reached
  :action - function to run when executing the task
  :goal-state - function to determine if the desired state is achieved. the action is not run if this is true
  :on-success - function to run if the function provided by :goal-state returns true
  :on-failure - function to run if the task runs out of attempts

  The return value of :action is passed into goal-state, on-success and on-failure."

  [task-name args & {:keys [desc priority max-attempts goal-state action on-success on-failure]}]
  `(defn ~task-name ~args
     (let [full-name# (name '~task-name)
           full-desc# (str ~@desc)]
       {:name full-name#
        :desc full-desc#
        :priority ~priority
        :max-attempts ~max-attempts
        :action (fn [] (do ~action))
        :goal-state (fn [~'result] (do ~goal-state))
        :on-success (fn [~'result] (do ~on-success))
        :on-failure (fn [~'result] (do ~on-failure))
        })))

(defn run-task
  ([task]
   (run-task task 1))
  ([task attempts]
   (println (str "Attempt " attempts))
   (let [{:keys [name desc priority max-attempts action goal-state on-success on-failure]} task
         result (action)]
     (cond
       (goal-state result)            (do
                                        (println (str "SUCCESS:  " name))
                                        (when on-success
                                          (on-success result)))
       (and max-attempts
            (= attempts max-attempts)) (do
                                        (println (str "FAILURE:  " name))
                                        (when on-failure
                                          (on-failure result)))

       :else                          (do
                                        (println (str "Retrying: " name))
                                        (recur task (+ 1 attempts))) ))))

(defn list-tasks
  ([] (list-tasks battle-tasks))
  ([queue]
   (println)
   (println "===== Tasks =====")
   (if (empty? @queue)
     (println "No tasks running.")
     (do
       (println "Running: " (:name (first (peek @queue))) "----" (:desc (first (peek @queue))))
       (doseq-indexed i [task (rest @queue)]
                      (println i ": " (:name (first task)) "----" (:desc (first task))))))


   (println "=================")
   (println)))
