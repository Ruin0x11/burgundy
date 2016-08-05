(ns burgundy.repl
  (:require [clojure.tools.nrepl.server :as repl]))

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
