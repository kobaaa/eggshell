(ns eggshell.graph
  (:refer-clojure :exclude [range])
  (:require [clojure.core :as core]
            [eggshell :as e]
            [rakk.core :as rakk]
            [loom.graph :as loom]
            [loom.attr :as attr]))


(defn make [] (loom/digraph))


(defn value [g cell]
  (rakk/value g cell))


(defn function? [g cell]
  (try
    (some? (attr/attr g cell ::e/raw-code))
    (catch Exception _ false)))


(defn functions [g]
  (filter (partial function? g) (loom/nodes g)))


(defn raw-code [g cell]
  (when (loom/has-node? g cell)
    (attr/attr g cell ::e/raw-code)))


(defn incoming-edges [g cell]
  (filter #(-> % second (= cell)) (loom/edges g)))


(defn set-function [g {:keys [cell code raw-code]}]
  (-> g
      (rakk/set-function cell (eval code))
      (attr/add-attr cell ::e/code code)
      (attr/add-attr cell ::e/raw-code raw-code)))


(defn- set-function-and-connect [g {:keys [cell inputs error raw-code] :as fun}]
  (if error
    (-> g
        (rakk/set-error cell error)
        (rakk/set-value cell :rakk/error)
        (rakk/set-function cell :rakk/error)
        (attr/add-attr cell ::e/raw-code raw-code))
    (let [g     (set-function g fun)
          edges (incoming-edges g cell)
          g     (if (seq edges)
                  (apply loom/remove-edges g edges)
                  g)]
      (apply loom/add-edges g (for [input inputs] [input cell])))))


(defn- inputs [function]
  (or (not-empty (:inputs function))
      [(:cell function)])) ;;no-input functions are an extra start themselves


(defn clear-functions
  "Clean eggshell-specific keys of cells"
  [g cells]
  (reduce (fn [g cell]
            (if-not (loom/has-node? g cell)
              g
              (-> g
                  (attr/remove-attr cell ::e/code)
                  (attr/remove-attr cell ::e/raw-code))))
          g cells))


(defn advance
  ([g new-inputs]
   (advance g new-inputs []))
  ([g new-inputs new-functions]
   (let [g (reduce (fn [g function]
                     (set-function-and-connect g function))
                   g new-functions)
         g (clear-functions g (keys new-inputs))]
     (rakk/advance g new-inputs (set (mapcat inputs new-functions))))))


(defn idx->column* [x] ;;TODO this is wrong beyond 702, fix
  (let [rem (mod x 26)
        div (/ x 26)]
    (if (< div 1)
      (str (char (+ 97 x)))
      (str (char (+ 97 (dec div))) (idx->column* rem)))))


(def idx->column (memoize idx->column*))


(defn coords->id* [row col]
  (keyword (str (idx->column col) row)))


(def coords->id (memoize coords->id*))


(defn ord [c] (- (int c) 96))


(defn col->idx [col]
  (dec
   (reduce
    +
    (map *
         (map ord (reverse col))
         (iterate #(* 26 %) 1)))))


(defn id->coords [id]
  (let [[_ col row] (re-find #"([a-z]+)([0-9]+)" (name id))]
    [(Integer/parseInt row) (col->idx col)]))


(defn cell-in-direction [id direction]
  (let [[row col] (id->coords id)]
    (apply coords->id
           (condp = direction
             :up    [(dec row) col]
             :down  [(inc row) col]
             :left  [row (dec col)]
             :right [row (inc col)]))))


(defn slice [g id1 id2]
  "When passed a graph and start and end cells, construct a seq or a
  seq of seqs (depending on the dimensionality of the slice) of the
  values of cells in the slice."
  (let [[row1 col1] (id->coords id1)
        [row2 col2] (id->coords id2)]
    (cond (= row1 row2)
          (for [col (core/range (min col1 col2) (inc (max col1 col2)))]
            (value g (coords->id row1 col)))

          (= col1 col2)
          (for [row (core/range (min row1 row2) (inc (max row1 row2)))]
            (value g (coords->id row col1)))

          :else
          (for [row (core/range (min row1 row2) (inc (max row1 row2)))]
            (for [col (core/range (min col1 col2) (inc (max col1 col2)))]
              (value g (coords->id row col)))))))


(defn map-slice
  "When passed a map that contains the values of cells (as happens in
  dataflow), and start and end cells, construct a seq or a seq of
  seqs (depending on the dimensionality of the slice) of the values in
  the map."
  [m id1 id2]
  (let [[row1 col1] (id->coords id1)
        [row2 col2] (id->coords id2)]
    (cond (= row1 row2)
          (for [col (core/range (min col1 col2) (inc (max col1 col2)))]
            (get m (coords->id row1 col)))

          (= col1 col2)
          (for [row (core/range (min row1 row2) (inc (max row1 row2)))]
            (get m (coords->id row col1)))

          :else
          (for [row (core/range (min row1 row2) (inc (max row1 row2)))]
            (for [col (core/range (min col1 col2) (inc (max col1 col2)))]
              (get m (coords->id row col)))))))


(defn slice->ids [id1 id2]
  (let [[row1 col1] (id->coords id1)
        [row2 col2] (id->coords id2)]
    (flatten
     (for [row (core/range (min row1 row2) (inc (max row1 row2)))]
       (for [col (core/range (min col1 col2) (inc (max col1 col2)))]
         (coords->id row col))))))
