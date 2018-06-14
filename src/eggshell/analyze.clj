(ns eggshell.analyze
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.ast :as ast]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.tools.analyzer.passes.jvm.emit-form :refer [emit-form]]))

;; (set! *print-length* 20)
;; (set! *print-level* 6)

(defn cell-ref-sym?
  "Does the symbol look like a cell reference?"
  [x]
  (some? (re-matches #"[a-z]+[0-9]+" (name x))))

(defn unresolvable-symbol-handler
  [class field ast]
  (if (cell-ref-sym? field)
    {:op    ::cell-ref
     ::cell field}
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

(defn cell-refs
  "All the cell refs in the AST"
  [ast]
  (->> ast ast/nodes (filter ::cell) distinct (map ::cell)))

(defn replace-cell-refs
  "Replace all cell refs with a lookups in the `cells` map."
  [ast]
  (ast/postwalk ast (fn [x] (if (::cell x) (ast-get-cell (::cell x)) x))))

(defn compile
  "\"Compile\" the passed AST to a function form."
  [ast]
  `(fn [~'cells] ~(emit-form (replace-cell-refs ast))))

;;(analyze '(+ a5 3))
;;(cell-refs (analyze '(+ a5 3)))

;;(compile (analyze '(let [x3 100] (+ x3 x4 100))))
;;(compile (analyze '(let [x3 100] (+ x3 x4 x5 100))))
;;((eval (compile (analyze '(let [x3 100] (+ x3 x4 x5 100))))) {:x4 4 :x5 2})
