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
  (let [g     (-> g
                  (rakk/set-function cell (eval code))
                  (attr/add-attr cell ::code code))
        edges (incoming-edges g cell)
        g     (if (seq edges)
                (apply loom/remove-edges g cell edges)
                g)]

    (apply loom/add-edges g (for [input inputs] [input cell]))))


(comment
  (-> (init)
      (set-value :a1 10)
      (set-function :a2
                    '(fn [{:keys [a1]}] (* a1 2))
                    [:a1])
      (rakk/init)))
