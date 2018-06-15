(ns eggshell.state
  (:require [eggshell.graph :as graph]))


(def graph-atom (atom (graph/make)))

(atom
 {:eggs {"uuid" (atom {:id       "uuid"
                       :filename "my.egg"
                       :aliases  []
                       :deps     {}
                       :graph    {}})}})
