(ns eggshell.graph-test
  (:require [eggshell.graph :as sut]
            [clojure.test :as test :refer [deftest testing is run-tests]]))

(deftest test-slice
  (testing "same column"
    (is (= '[a1 a2 a3 a4 a5 a6 a7 a8 a9 a10] (sut/slice 'a1 'a10)))
    (is (= '[a1 a2 a3 a4 a5 a6 a7 a8 a9 a10] (sut/slice 'a10 'a1)))
    (is (= (sut/slice 'a1 'a10)
           (sut/slice 'a10 'a1))))

  (testing "same row"
    (is (= '[a0 b0 c0 d0 e0 f0 g0 h0 i0 j0] (sut/slice 'a0 'j0)))
    (is (= '[a0 b0 c0 d0 e0 f0 g0 h0 i0 j0] (sut/slice 'j0 'a0)))
    (is (= (sut/slice 'a0 'j0)
           (sut/slice 'j0 'a0))))

  (testing "rectangle"
    (is (= '[[a0 b0 c0 d0 e0]
             [a1 b1 c1 d1 e1]
             [a2 b2 c2 d2 e2]
             [a3 b3 c3 d3 e3]
             [a4 b4 c4 d4 e4]]
           (sut/slice 'a0 'e4)))
    (is (= (sut/slice 'a0 'e4) (sut/slice 'e4 'a0)))))
