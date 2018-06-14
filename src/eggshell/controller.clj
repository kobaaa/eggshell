(ns eggshell.controller
  (:require [eggshell.io.v1 :as io]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [loom.graph :as loom]
            [loom.attr :as attr]
            [clojure.string :as str]
            [clojure.edn :as edn]))


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
       (reset! graph/graph-atom))
  nil)


;;TODO error handling
(defn- input->value [s]
  (let [r (try (edn/read-string s)
               (catch Exception _ ::could-not-read))]
    (cond (or (str/starts-with? s "[")
              (str/starts-with? s "{")
              (str/starts-with? s "#{")
              (number? r))
          r
          :else
          s)))


(defn set-cell-at! [g [row col] value]
  (let [cell-id (graph/coords->id row col)]
    (if (str/starts-with? value "(")
      (let [code (read-string value)
            ast  (analyze/analyze code)]
        (swap! g graph/advance {} [{:cell     cell-id
                                    :inputs   (map keyword (analyze/cell-refs ast))
                                    :raw-code value
                                    :code     (analyze/compile ast)}]))
      (swap! g graph/advance {cell-id (input->value value)} []))))
