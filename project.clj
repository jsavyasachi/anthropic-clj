(defproject net.clojars.savya/anthropic-sdk-clj "0.1.0-SNAPSHOT"
  :description "Idiomatic Clojure wrapper over the official Anthropic Java SDK"
  :url "https://github.com/jsavyasachi/anthropic-sdk-clj"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [com.anthropic/anthropic-java "2.40.1"]]
  :global-vars {*warn-on-reflection* true})
