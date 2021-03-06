(ns pallet.common.context-test
  (:require
   [pallet.common.context :as context]
   [pallet.common.logging.logutils :as logutils])
  (:use
   clojure.test))

(deftest in-context-test
  (testing "single level"
    (context/in-context
     "fred" {}
     (is (= ["fred"] (context/formatted-context-entries))))
    (is (not (bound? #'context/*current-context*))))
  (testing "nested levels"
    (context/in-context
     "fred" {}
     (is (= ["fred"] (context/formatted-context-entries)))
     (context/in-context
      "bloggs" {}
      (is (= ["fred" "bloggs"] (context/formatted-context-entries))))
     (is (= ["fred"] (context/formatted-context-entries))))
    (is (not (bound? #'context/*current-context*))))
  (testing "non-default scope"
    (context/in-context
     "fred" {:scope :fred}
     (is (= ["fred"] (context/formatted-context-entries))))
    (is (not (bound? #'context/*current-context*))))
  (testing "non-default context"
    (let [context (context/make-context)]
      (context/in-context
       "fred" {:context context}
       (is (= ["fred"] (context/formatted-context-entries))))
      (is (not (bound? #'context/*current-context*))))))

(deftest on-enter-test
  (let [c (context/on-enter
           (context/make-context :on-enter #(assoc % :a %2)) :b)]
    (is (= :b (:a c))))
    (let [c (context/on-enter
             (update-in
              (context/make-context :on-enter #(assoc % :a %2))
              [:on-enter] conj #(assoc % :b %2))
             :b)]
      (is (= :b (:a c)))
      (is (= :b (:b c)))))

(deftest in-context-with-callback-test
  (let [seen-enter (atom nil)
        seen-exit (atom nil)]
    (context/in-context
     "fred" {:on-enter (fn on-enter [context value]
                         (is (not=
                              :pallet.common.context/default
                              (:pallet.common.context/current-scope context)))
                         (is (= value "fred"))
                         (is (not @seen-enter))
                         (is (not @seen-exit))
                         (reset! seen-enter true)
                         nil)
             :on-exit (fn on-exit [context value]
                        (is (not=
                             :pallet.common.context/default
                              (:pallet.common.context/current-scope context)))
                        (is (= value "fred"))
                        (is @seen-enter)
                        (is (not @seen-exit))
                        (reset! seen-exit true)
                        nil)}
     (is (= ["fred"] (context/formatted-context-entries)))
     (is @seen-enter)
     (is (not @seen-exit)))
    (is (not (bound? #'context/*current-context*)))
    (is @seen-enter)
    (is @seen-exit)))

(deftest options-test
  (let [c (context/options {} {:format str})]
    (is (= str (:format c)))
    (is (:pallet.common.context/current-scope c))))

(deftest context-with-format-test
  (context/in-context
   "fred" {:format #(apply str (reverse %))}
   (is (= ["derf"] (context/formatted-context-entries)))
   (is (= ["derf"] (context/formatted-scope-entries)))))

(deftest try-context-test
  (try
   (context/in-context
    "fred" {}
    (context/try-context
     (throw (Exception. "msg"))))
   (catch clojure.lang.ExceptionInfo e
     (is (= ["fred"] (context/formatted-context-entries
                      (:context (ex-data e)))))
     (is (= "msg" (:message (ex-data e))))))
  (try
   (context/in-context
    "fred" {:on-exception (fn on-exception-fn [context exception-map]
                            (assoc
                                exception-map :msg
                                (context/formatted-context-entries
                                 context)))}
    (context/try-context {}
     (throw (Exception. "msg"))))
   (catch clojure.lang.ExceptionInfo e
     (is (= ["fred"] (:context (ex-data e))))
     (is (= ["fred"] (:msg (ex-data e)))))))

(deftest with-context-test
  (try
   (context/with-context "fred" {}
     (throw (Exception. "msg")))
   (catch clojure.lang.ExceptionInfo e
     (is (= ["fred"] (:context (ex-data e)))))))

(deftest with-logged-context-test
  (is (= "debug fred\n"
         (logutils/logging-to-string
          (context/with-logged-context "fred" {}
            nil))))
  (is (= "info fred\n"
         (logutils/logging-to-string
          (context/with-logged-context "fred" {:log-level :info}
            nil)))))

(deftest with-context-logging-test
  (is (= "info -> fred\ninfo <- fred\n"
         (logutils/logging-to-string
           (context/with-context-logging
             (context/with-context "fred"
               nil))))))

(deftest context-history-test
  (let [c ((context/context-history {}) (context/make-context) "c")]
    (is (= :history (:pallet.common.context/history-kw c))
        "should assoc the history key")
    (is (:pallet.common.context/scope-options (first (:history c)))
        "should store the context")))

(deftest with-context-history-test
  (context/with-context nil {:scope :s :on-enter (context/context-history {})}
    (context/with-context "fred" {}
      (context/with-context "blogs" {}
        (is (=
             ["fred" "blogs"]
             (context/formatted-history context/*current-context*)))))))

(deftest scope-context-entries-test
  (context/with-context "a" {}
    (context/with-context "b" {:scope :s}
      (context/with-context "c" {}
        (context/with-context "d" {:scope :s1}
          (is (=
               ["b" "c"]
               (context/scope-context-entries :s)))
          (is (=
               ["b" "c"]
               (context/scope-formatted-context-entries :s))))))))
