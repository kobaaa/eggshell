(ns eggshell.state
  (:require [eggshell :as e]
            [eggshell.graph :as graph]
            [clojure.string :as str]))

(def default-aliases
  (str/join
   \newline
   ["egg  eggshell.api"
    "math eggshell.api.math"
    "img  eggshell.api.img"
    "str  clojure.string"
    "set  clojure.set"
    "io   clojure.java.io"
    "fs   me.raynes.fs"]))


(def egg-atom (atom {::e/graph   (graph/make)
                     ::e/deps    "{}"
                     ::e/aliases default-aliases}))

(atom
 {:eggs {"uuid" (atom {::e/id       "uuid"
                       ::e/filename "my.egg"
                       ::e/aliases  []
                       ::e/deps     {}
                       ::e/graph    {}
                       ::e/gui      {:column-widths []}})}})
