(ns eggshell.state
  (:require [eggshell.graph :as graph]
            [clojure.string :as str]))

(def default-aliases
  (str/join
   \newline
   ["egg  eggshell.api"
    "math eggshell.api.math"
    "str  clojure.string"
    "set  clojure.set"]))


(def egg-atom (atom {:graph   (graph/make)
                     :aliases default-aliases}))

(atom
 {:eggs {"uuid" (atom {:id       "uuid"
                       :filename "my.egg"
                       :aliases  []
                       :deps     {}
                       :graph    {}
                       :gui      {:column-widths []}})}})
