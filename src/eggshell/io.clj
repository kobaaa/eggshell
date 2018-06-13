(ns eggshell.io)

(defn assert-version [v x]
  (assert (= v (:eggshell/version x)))
  x)
