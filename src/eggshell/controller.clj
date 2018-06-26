(ns eggshell.controller
  (:require [eggshell.io.v1 :as io]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [loom.graph :as loom]
            [loom.attr :as attr]
            [rakk.core :as rakk]
            [clojure.string :as str]
            [clojure.edn :as edn]))


(defn- compile-function [g n aliases]
  (if-let [raw-code (attr/attr g n ::graph/raw-code)]
    (let [ast  (analyze/analyze (read-string raw-code) {:aliases aliases})
          code (analyze/compile ast)]
      (-> g
          (attr/add-attr n :function (eval code))
          (attr/add-attr n ::code code)))
    g))


(defn- compile-functions [g aliases]
  (reduce #(compile-function %1 %2 aliases) g (loom/nodes g)))


(defn load-egg [filename {:keys [state-atom]}]
  (let [{:eggshell/keys [graph aliases]} (io/load-egg filename)]
    (reset! state-atom
            {:graph   (compile-functions graph aliases)
             :aliases aliases}))
  nil)


(defn save-egg [filename {:keys [state]}]
  (io/save-egg state filename))


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


(defn- compile [code aliases]
  (try
    (let [ast (analyze/analyze code aliases)]
      {:inputs (map keyword (analyze/cell-refs ast))
       :code   (analyze/compile ast)})))


(defn- set-function-at! [state-atom cell-id [row col] value]
  (let [parsed                (read-string value)
        {:keys [inputs code]} (compile parsed (:aliases @state-atom))]
    (swap! state-atom update :graph graph/advance {}
           [{:cell     cell-id
             :inputs   inputs
             :raw-code value
             :code     code}])))


(defn set-cell-at! [state-atom [row col] value]
  (let [cell-id (graph/coords->id row col)]
    (if (str/starts-with? value "(")
      (set-function-at! state-atom cell-id [row col] value)
      (swap! state-atom update :graph graph/advance {cell-id (input->value value)} []))))


(defn render-value [x]
  (cond (= x :rakk/error) "ERROR!"
        (seq? x) (pr-str (doall x))
        (string? x) x
        (boolean? x) (pr-str x)
        (nil? x) ""
        :else (pr-str x)))


(defn get-value-at [state-atom [row col]]
  (let [g (:graph @state-atom)]
    (if (= -1 col)
      {:render-value (str row)}
      (let [cell-id (keyword (graph/coords->id row col))]
        (let [v (graph/value g cell-id)]
          (merge
           {:original-value v
            :render-value   (render-value v)
            :cell-id        cell-id}
           (rakk/error-info g cell-id)))))))


(defn get-editable-value-at [state-atom [row col]]
  (let [g       (:graph @state-atom)
        cell-id (keyword (graph/coords->id row col))]
    (if (graph/function? g cell-id)
      (graph/raw-code g cell-id)
      (if-let [v (graph/value g cell-id)]
        (cond (string? v)
              v
              :else
              (pr-str v))
        nil))))
