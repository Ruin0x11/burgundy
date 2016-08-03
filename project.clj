(defproject burgundy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot burgundy.core
  :target-path "target/%s"
  ;; :jvm-opts ["-XX:OnError=gdb - %p" "-Djava.library.path=ppsspp/build/lib"]
  :jvm-opts ["-Djava.library.path=ppsspp/build/lib"]
  :resource-paths ["java/dist/psp.jar"]
  :profiles {:uberjar {:aot :all}})
