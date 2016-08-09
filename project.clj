(defproject burgundy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.priority-map "0.0.7"]
                 [wharf "0.2.0-SNAPSHOT"]]
  :main ^:skip-aot burgundy.core
  :target-path "target/%s"
  ;; :jvm-opts ["-XX:OnError=gdb - %p" "-Djava.library.path=ppsspp/build/lib"]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow" "-Djava.library.path=ppsspp/build/lib"]
  :resource-paths ["java/dist/psp.jar"]
  :profiles {:uberjar {:aot :all}}
  :aliases {"ai" ["exec" "-ep" "(use 'burgundy.core) (swap! run-ai? (fn [_] true)) (-main)"]}

  :repl-options { ;; Specify the string to print when prompting for input.
                 ;; defaults to something like (fn [ns] (str *ns* "=> "))
                 :prompt (fn [ns] (str "your command for <" ns ">, master? " ))
                 ;; What to print when the repl session starts.
                 :welcome (println "Welcome to the magical world of the repl!")})
