(ns eggshell.controller
  (:require [eggshell.io.v1 :as io]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [loom.graph :as loom]
            [loom.attr :as attr]))


(defn- compile-function [g n]
  (if-let [raw-code (attr/attr g n ::graph/raw-code)]
    (let [ast  (analyze/analyze (read-string raw-code))
          code (analyze/compile ast)]
      (-> g
          (attr/add-attr n :function (eval code))
          (attr/add-attr n ::code code)))
    g))


(defn- compile-functions [g]
  (reduce compile-function g (loom/nodes g)))


(defn load-eggshell [filename]
  (->> (io/load-eggshell filename)
       :eggshell/graph
       compile-functions
       (reset! graph/graph-atom)))
