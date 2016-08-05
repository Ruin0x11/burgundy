(ns burgundy.repl
  (:require [clojure.tools.nrepl.server :as repl]))

(def repl-command (atom (clojure.lang.PersistentQueue/EMPTY)))
(def repl-result (atom (clojure.lang.PersistentQueue/EMPTY)))

(defn dequeue!
  [queue]
  (loop []
    (let [q     @queue
          value (peek q)
          nq    (pop q)]
      (if (compare-and-set! queue q nq)
        value
        (recur)))))

(defmacro cmd
  "For REPL use. Wrap a form to be executed during the gameUpdate
  loop."
  [& body]
  `(do (swap! repl-command conj (fn [] (do ~@body)))
       (loop []
         (if-let [result# (dequeue! repl-result)]
           (:result result#)
           (recur)))))

(def repl-server (atom nil))
(def repl-control (atom false))

(defn start-repl! [port]
  (reset! repl-server (repl/start-server :port port)))

(defn stop-repl! []
  (repl/stop-server @repl-server)
  (reset! repl-server nil))

(defn repl-control! [bool-or-kw]
  (cond
   (= :toggle bool-or-kw) (recur (not @repl-control))
   :else (do (reset! repl-control bool-or-kw)
             (println (str "REPL control is " (if bool-or-kw "ENABLED" "DISABLED"))))))

(defn execute-repl-queue []
  (when-let [command (dequeue! repl-command)]
    (try
      (let [result (command)]
        (swap! repl-result conj {:result result}))
      (catch Exception e
        (swap! repl-result conj {:result e})))))
