(ns eggshell.grid-test
  (:require [clojure.test :refer [deftest testing is] :as test]
            [rakk.core :as rakk]
            [eggshell.grid :as sut]))


(deftest advance-test

  (testing "chained functions"
    (is (= {:a1 10
            :a2 20
            :a3 220}
           (-> (sut/init)
               (sut/advance {:a1 10})
               (sut/advance {} [{:cell   :a2
                                 :code   '(fn [{:keys [a1]}] (* a1 2))
                                 :inputs [:a1]}])
               (sut/advance {} [{:cell   :a3
                                 :code   '(fn [{:keys [a2]}] (+ a2 200))
                                 :inputs [:a2]}])
               rakk/values))))

  (testing "chained functions that are redefined (same chain)"
    (is (= {:a1 10
            :a2 50
            :a3 250}
           (-> (sut/init)
               (sut/advance {:a1 10})
               (sut/advance {} [{:cell   :a2
                                 :code   '(fn [{:keys [a1]}] (* a1 2))
                                 :inputs [:a1]}])
               (sut/advance {} [{:cell   :a3
                                 :code   '(fn [{:keys [a2]}] (+ a2 200))
                                 :inputs [:a2]}])
               (sut/advance {} [{:cell   :a2
                                 :code   '(fn [{:keys [a1]}] (* a1 5))
                                 :inputs [:a1]}])
               rakk/values))))

  (testing "chained functions that are redefined to rely on different input"
    (is (= {:a1 10
            :b1 30
            :a2 150
            :a3 350}
           (-> (sut/init)
               (sut/advance {:a1 10 :b1 30})
               (sut/advance {} [{:cell   :a2
                                 :code   '(fn [{:keys [a1]}] (* a1 2))
                                 :inputs [:a1]}])
               (sut/advance {} [{:cell   :a3
                                 :code   '(fn [{:keys [a2]}] (+ a2 200))
                                 :inputs [:a2]}])
               (sut/advance {} [{:cell   :a2
                                 :code   '(fn [{:keys [b1]}] (* b1 5))
                                 :inputs [:b1]}])
               rakk/values)))))
