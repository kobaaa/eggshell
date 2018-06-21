(ns eggshell.analyze
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.ast :as ast]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.tools.analyzer.passes.jvm.emit-form :refer [emit-form]]
            [clojure.string :as str]
            [eggshell.graph :as graph]))

;; (set! *print-length* 20)
;; (set! *print-level* 6)

(defn cell-ref-sym?
  "Does the symbol look like a cell reference?"
  [x]
  (some? (re-matches #"[a-z]+[0-9]+" (name x))))


(defn cell-slice-sym?
  "Does the symbol look like a cell slice?"
  [x]
  (some? (re-matches #"[a-z]+[0-9]+:[a-z]+[0-9]+" (name x))))


(defn split-cell-slice [sym]
  (map symbol (str/split (name sym) #":")))


(defn unresolvable-symbol-handler
  [class field ast]
  (cond (cell-ref-sym? field)
        {:op    ::cell-ref
         ::cell field}

        (cell-slice-sym? field)
        (let [[cell1 cell2] (split-cell-slice field)]
          {:op     ::cell-slice
           ::cell1 cell1
           ::cell2 cell2})

        :else
        (throw (ex-info (format "Could not resolve symbol %s" (name field))
                        {:field field :ast ast}))))


(defn analyze
  "Analyze form to produce AST. Unknown names that look like cell refs
  are resolved as such."
  [form]
  (ana.jvm/analyze form
                   (ana.jvm/empty-env)
                   {:passes-opts
                    (merge ana.jvm/default-passes-opts
                           {:validate/unresolvable-symbol-handler unresolvable-symbol-handler})}))


(defn ast-get-cell
  "Produce AST fragment that corresponds to looking up a cell in the
  `cells` map. Used for code generation of the function of a cell."
  [cell-ref]
  (let [cell (keyword cell-ref)]
    {:op     :static-call
     :method 'get
     :class  clojure.lang.RT
     :args   [{:name  'cells
               :op    :local
               :form  'cells
               :local :let}
              {:op       :const
               :type     :keyword
               :literal? true
               :val      cell
               :form     cell}]}))


(defn ast-slice
  [cell1 cell2]
  (let [cell1 (keyword cell1)
        cell2 (keyword cell2)]
   {:op   :invoke
    :fn   {:op   :var
           :form 'eggshell.graph/map-slice}
    :args [{:name  'cells
            :op    :local
            :form  'cells
            :local :let}
           {:op       :const
            :type     :keyword
            :literal? true
            :val      cell1
            :form     cell1}
           {:op       :const
            :type     :keyword
            :literal? true
            :val      cell2
            :form     cell2}]}))


(defn replace-cell-slices
  "Replace all cell refs with a lookups in the `cells` map."
  [ast]
  (ast/postwalk
   ast
   (fn [x]
     (if (some-> x :op (= ::cell-slice))
       (ast-slice (::cell1 x) (::cell2 x))
       x))))


(defn replace-cell-refs
  "Replace all cell refs with a lookups in the `cells` map."
  [ast]
  (ast/postwalk ast (fn [x] (if (::cell x) (ast-get-cell (::cell x)) x))))


(defn compile
  "\"Compile\" the passed AST to a function form."
  [ast]
  `(fn [~'cells] ~(-> ast ;;TODO gensym the cells param
                      replace-cell-slices
                      replace-cell-refs
                      emit-form)))


(defn cell-refs
  "All the cell refs in the AST"
  [ast]
  (let [nodes (ast/nodes ast)]
    (distinct
     (concat (->> nodes (filter ::cell) (map ::cell))
             (some->> nodes
                      (filter ::cell1)
                      not-empty
                      (map (juxt ::cell1 ::cell2))
                      (mapcat (partial apply graph/slice->ids))
                      (map (comp symbol name)))))))


;;(analyze '(+ a5 3))
;;(cell-refs (analyze '(+ a5 3)))

;;(-> (analyze '(+ a1 t5 b0:b5 a0:a8 b1)) (cell-refs))

;;(compile (analyze '(let [x3 100] (+ x3 x4 100))))
;;(compile (analyze '(let [x3 100] (+ x3 x4 x5 100))))
;;((eval (compile (analyze '(let [x3 100] (+ x3 x4 x5 100))))) {:x4 4 :x5 2})


;; TODO
;; currently, cells-refs (which is used to figure out the dependencies
;; of a cell) is a bit wasteful because it expands to a long list of
;; all the cells that are referred to explicitely or as a slice. It
;; would be ideal to modify the dataflow algorithm to understand
;; slices to avoid their wasteful expansion.
