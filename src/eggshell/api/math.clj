(ns eggshell.api.math
  (:require [clojure.core :as core])
  (:refer-clojure :exclude [+ count]))

(defn + [cells]
  (if (seq? (first cells))
    (map + cells)
    (apply core/+ (remove nil? cells))))


(def sum +)


(defn count [cells]
  (if (seq? (first cells))
    (map count cells)
    (core/count (remove nil? cells))))


(defn avg [cells]
  (float
   (/ (sum cells)
      (count cells))))
