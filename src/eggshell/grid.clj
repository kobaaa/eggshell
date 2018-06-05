(ns eggshell.grid
  (:require [rakk.core :as rakk]
            [loom.graph :as loom]
            [loom.attr :as attr]))


(defn init [] (loom/digraph))


(defn set-value [g cell value]
  (-> g
      (rakk/clear-function cell)
      (rakk/set-value cell value)))


(defn incoming-edges [g cell]
  (filter #(-> % second (= cell)) (loom/edges g)))


(defn set-function [g cell code inputs]
  (let [g (-> g
              (rakk/set-function cell (eval code))
              (attr/add-attr cell ::code code))
        g (apply loom/remove-edges g cell (incoming-edges g))]

    (apply loom/add-edges g (for [input inputs] [input cell]))))
