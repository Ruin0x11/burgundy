(ns burgundy.repl
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [clojure.tools.nrepl :as repl]))

(def repl-server (atom nil))
(def repl-control (atom false))

(defn start-repl! [port]
  (reset! repl-server (start-server :port port))
  (with-open [conn (repl/connect :port port)]
    (-> (repl/client conn 1000)
        ;; load namespaces automatically, to save retyping them
        (repl/message {:op :eval :code "(use '(burgundy interop battle menu queue core))"})
        doall
        clojure.pprint/pprint)))

(defn stop-repl! []
  (stop-server @repl-server)
  (reset! repl-server nil))

(defn repl-control! [bool-or-kw]
  (cond
    (= :toggle bool-or-kw) (recur (not @repl-control))
    :else (do (reset! repl-control bool-or-kw)
              (println (str "REPL control is " (if bool-or-kw "ENABLED" "DISABLED"))))))
