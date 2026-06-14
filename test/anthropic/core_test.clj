(ns anthropic.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [anthropic.core :as a])
  (:import (com.anthropic.models.messages MessageCreateParams)))

(def ->params #'a/->params)

(deftest request-translation
  (testing "a request map becomes MessageCreateParams with the given model + max-tokens"
    (let [^MessageCreateParams p (->params {:model "claude-sonnet-4-6"
                                            :max-tokens 512
                                            :system "be terse"
                                            :messages [{:role :user :content "hi"}]})]
      (is (= "claude-sonnet-4-6" (str (.model p))))
      (is (= 512 (.maxTokens p)))))
  (testing "defaults: opus-4-8 and 1024 max-tokens"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]})]
      (is (= "claude-opus-4-8" (str (.model p))))
      (is (= 1024 (.maxTokens p))))))

;; Live round-trip — only runs when ANTHROPIC_API_KEY is set (network + billed).
(deftest ^:integration create-message-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [resp (a/create-message (a/client)
                                 {:model "claude-haiku-4-5"
                                  :max-tokens 16
                                  :messages [{:role :user :content "Reply with the single word: pong"}]})]
      (is (= :assistant (:role resp)))
      (is (= :text (:type (first (:content resp)))))
      (is (string? (:text (first (:content resp)))))
      (is (pos? (-> resp :usage :output-tokens))))))

(deftest ^:integration stream-text-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [deltas (atom [])
          full (a/stream-text (a/client)
                              {:model "claude-haiku-4-5"
                               :max-tokens 16
                               :messages [{:role :user :content "Reply with the single word: pong"}]}
                              #(swap! deltas conj %))]
      (is (string? full))
      (is (pos? (count full)))
      (is (= full (apply str @deltas))))))
