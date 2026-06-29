(defproject net.clojars.savya/anthropic-clj "0.6.5"
  :description "Idiomatic Clojure wrapper over the official Anthropic Java SDK"
  :url "https://github.com/jsavyasachi/anthropic-clj"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [com.anthropic/anthropic-java "2.45.0"]
                 [metosin/jsonista "1.0.0"]]
  :global-vars {*warn-on-reflection* true}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}
  :profiles {:clojure-1-10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :clojure-1-11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :clojure-1-12 {:dependencies [[org.clojure/clojure "1.12.5"]]}}
  :aliases {"all" ["with-profile" "+clojure-1-10:+clojure-1-11:+clojure-1-12"]})
