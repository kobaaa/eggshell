(ns eggshell.controller
  (:require [eggshell :as e]
            [eggshell.io.v1 :as io]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [loom.graph :as loom]
            [loom.attr :as attr]
            [rakk.core :as rakk]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:refer-clojure :exclude [compile]))


(defn load-egg [filename {:keys [state-atom]}]
  (let [{:eggshell/keys [graph aliases]} (io/load-egg filename)]
    ;; (reset! state-atom
    ;;         {::e/graph   (compile-functions graph aliases)
    ;;          ::e/aliases aliases})
    )
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


(defn- parse-aliases [s]
  (->> s
       str/split-lines
       (remove #(str/starts-with? % ";"))
       (remove empty?)
       (map #(str/split % #"\s+"))
       (into {})))


(defn- compile [string-code aliases cell-id] ;;TODO implement recompile-all based on this and on graph/advance
  (let [parsed (read-string string-code)]
   (try
     (let [ast (analyze/analyze parsed {::e/aliases (parse-aliases aliases)})]
       {:cell     cell-id
        :raw-code string-code
        :inputs   (map keyword (analyze/cell-refs ast))
        :code     (analyze/compile ast)})
     (catch Exception e
       {:cell     cell-id
        :raw-code string-code
        :inputs   []
        :error    e}))))


(defn- recompile-all [{:keys [graph aliases] :as state}]
  (let [function-cells (graph/functions graph)
        compiled       (map #(compile (graph/raw-code graph %) aliases %) function-cells)]
    (update state ::e/graph graph/advance {} compiled)))


(defn set-aliases! [state-atom aliases]
  (swap! state-atom
         #(-> %
              (assoc ::e/aliases aliases)
              recompile-all)))


(defn- set-function-at! [state-atom cell-id [row col] value]
  (let [compiled (compile value (::e/aliases @state-atom) cell-id)]
    (swap! state-atom update ::e/graph graph/advance {} [compiled])))


(defn set-cell-at! [state-atom [row col] value]
  (when value
    (let [cell-id (graph/coords->id row col)]
      (if (str/starts-with? value "(")
        (set-function-at! state-atom cell-id [row col] value)
        (swap! state-atom update ::e/graph graph/advance {cell-id (input->value value)} [])))))


(defn render-value [x]
  (cond
    (seq? x) (pr-str (doall x))
    (string? x) x
    (boolean? x) (pr-str x)
    (nil? x) ""
    :else (pr-str x)))


(defn get-value-at [state-atom [row col]]
  (let [g (::e/graph @state-atom)]
    (if (= -1 col)
      {:render-value (str row)}
      (let [cell-id (keyword (graph/coords->id row col))
            v       (graph/value g cell-id)]
        (merge
         {:original-value v
          :render-value   (if (rakk/error? g cell-id)
                            "ERROR!"
                            (render-value v))
          :cell-id        cell-id}
         (rakk/error-info g cell-id))))))


(defn get-editable-value-at [state-atom [row col]]
  (let [g       (::e/graph @state-atom)
        cell-id (keyword (graph/coords->id row col))]
    (if (graph/function? g cell-id)
      (graph/raw-code g cell-id)
      (if-let [v (graph/value g cell-id)]
        (cond (string? v)
              v
              :else
              (pr-str v))
        nil))))
