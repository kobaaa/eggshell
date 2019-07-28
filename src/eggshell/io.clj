(ns eggshell.io)


(defn assert-version [x v]
  (assert (= v (:eggshell/version x)))
  x)



