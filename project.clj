(defproject net.clojars.savya/anthropic-clj "0.12.1"
  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)})
