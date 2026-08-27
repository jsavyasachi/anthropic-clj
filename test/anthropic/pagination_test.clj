(ns anthropic.pagination-test
  (:require [clojure.test :refer [deftest is]]
            [anthropic.core]
            [anthropic.beta]
            [anthropic.beta.messages]))

(deftest every-list-function-has-a-lazy-sibling
  (doseq [[namespace names]
          [['anthropic.core '[list-models list-batches list-files]]
           ['anthropic.beta '[list-beta-models list-skills list-skill-versions
                              list-memory-stores list-memories list-memory-versions
                              list-agents list-sessions list-session-events
                              list-session-threads list-thread-events list-session-resources
                              list-deployments list-deployment-runs list-environments
                              list-environment-work list-vaults list-tunnels
                              list-tunnel-certificates list-agent-versions list-dreams
                              list-vault-credentials list-user-profiles]]
           ['anthropic.beta.messages '[list-beta-batches]]]]
    (doseq [name names]
      (is (fn? (some-> (ns-resolve namespace (symbol (str name "-lazy"))) deref))
          (str namespace "/" name "-lazy must be public")))))

(deftest lazy-pager-does-not-realize-later-items
  (let [later-items (atom 0)
        pager (reify java.lang.Iterable
                (iterator [_]
                  (let [items (atom [{:id 1} {:id 2}])]
                    (reify java.util.Iterator
                      (hasNext [_] (boolean (seq @items)))
                      (next [_]
                        (let [item (first @items)]
                          (swap! items rest)
                          (when (= 2 (:id item))
                            (swap! later-items inc))
                          item))
                      (remove [_] (throw (UnsupportedOperationException.)))))))]
    (if-let [helper (some-> (find-ns 'anthropic.pagination)
                            (ns-resolve '->lazy-pager)
                            deref)]
      (do
        (is (fn? helper))
        (is (= [{:id 1}] (take 1 (helper identity pager))))
        (is (zero? @later-items)))
      (is false "anthropic.pagination/->lazy-pager must exist"))))
