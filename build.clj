(ns build
  "Build + Clojars deploy for anthropic-clj (tools.build + deps-deploy).

   Usage:
     clojure -T:build jar
     clojure -T:build deploy   ; needs CLOJARS_USERNAME / CLOJARS_PASSWORD"
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'net.clojars.savya/anthropic-clj)
(def version "0.16.0")
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"})
  (b/delete {:path "pom.xml"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]
                :scm {:url "https://github.com/jsavyasachi/anthropic-clj"
                      :connection "scm:git:https://github.com/jsavyasachi/anthropic-clj.git"
                      :developerConnection "scm:git:ssh://git@github.com/jsavyasachi/anthropic-clj.git"
                      :tag (str "v" version)}
                :pom-data [[:description "Idiomatic Clojure wrapper over the official Anthropic Java SDK"]
                           [:url "https://github.com/jsavyasachi/anthropic-clj"]
                           [:licenses
                            [:license
                             [:name "Eclipse Public License 2.0"]
                             [:url "https://www.eclipse.org/legal/epl-2.0/"]
                             [:distribution "repo"]]]]})
  (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
  (b/jar {:class-dir class-dir :jar-file jar-file})
  (println "Wrote" jar-file))

(defn deploy [_]
  (jar nil)
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))
