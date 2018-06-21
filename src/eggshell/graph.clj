(ns eggshell.graph
  (:refer-clojure :exclude [range])
  (:require [clojure.core :as core]
            [rakk.core :as rakk]
            [loom.graph :as loom]
            [loom.attr :as attr]))


(defn make [] (loom/digraph))


(defn set-value [g cell value]
  (-> g
      (rakk/clear-function cell)
      (rakk/set-value cell value)))


(defn value [g cell]
  (rakk/value g cell))


(defn function? [g cell]
  (try
    (some? (attr/attr g cell :function))
    (catch Exception _ false)))


(defn raw-code [g cell]
  (attr/attr g cell ::raw-code))


(defn incoming-edges [g cell]
  (filter #(-> % second (= cell)) (loom/edges g)))


(defn set-function [g {:keys [cell code raw-code]}]
  (-> g
      (rakk/set-function cell (eval code))
      (attr/add-attr cell ::code code)
      (attr/add-attr cell ::raw-code raw-code)))


(defn set-function-and-connect [g {:keys [cell inputs] :as fun}]
  (let [g     (set-function g fun)
        edges (incoming-edges g cell)
        g     (if (seq edges)
                (apply loom/remove-edges g edges)
                g)]
    (apply loom/add-edges g (for [input inputs] [input cell]))))


(defn advance
  ([g new-inputs]
   (advance g new-inputs []))
  ([g new-inputs new-functions]
   (let [g (reduce (fn [g function]
                     (set-function-and-connect g function))
                   g new-functions)]
     (rakk/advance g new-inputs (set (mapcat :inputs new-functions))))))


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


(defn in-line? [[row1 col1] [row2 col2]]
  (or (= row1 row2)
      (= col1 col2)))


(defn slice [g id1 id2]
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


(defn slice->ids [id1 id2]
  (let [[row1 col1] (id->coords id1)
        [row2 col2] (id->coords id2)]
    (flatten
     (for [row (core/range (min row1 row2) (inc (max row1 row2)))]
       (for [col (core/range (min col1 col2) (inc (max col1 col2)))]
         (coords->id row col))))))


(comment
  (-> (init)
      (set-value :a1 10)
      (set-function-and-connect
       :a2
       '(fn [{:keys [a1]}] (* a1 2))
       [:a1])
      (rakk/init)))


(comment
  (-> (init)
      (set-value :a1 10)
      (set-function-and-connect
       :a2
       '(fn [{:keys [a1]}] (* a1 2))
       [:a1])
      (rakk/init)
      (advance {:a1 4})
      (advance {:a1 8} [{:cell   :a2
                         :inputs [:a1]
                         :code   '(fn [{:keys [a1]}] (* a1 100))}])))

(comment
  (-> (init)
      (advance {:a1 10})
      (advance {} [{:cell   :a2
                    :code   '(fn [{:keys [a1]}] (* a1 2))
                    :inputs [:a1]}])
      (advance {} [{:cell   :a3
                    :code   '(fn [{:keys [a2]}] (+ a2 200))
                    :inputs [:a2]}])))

(comment
  (mutate! {:a0 20} [])
  (mutate! {:a1 10} [])
  (mutate! {} [{:cell :a2 :code '(fn [{:keys [a1]}] (* 3 a1)) :inputs #{:a1}}])
  (mutate! {} [{:cell :a3 :code '(fn [{:keys [a2]}] (* 3 a2)) :inputs #{:a2}}])
  (mutate! {:a1 55} [])
  )
