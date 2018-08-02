(ns eggshell.controller
  (:require [eggshell :as e]
            [eggshell.io.v1 :as io]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [loom.graph :as loom]
            [loom.attr :as attr]
            [rakk.core :as rakk]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.tools.deps.alpha.repl :as deps]

            [seesaw.core :as ss]
            [eggshell.gui.table :as table])
  (:refer-clojure :exclude [compile]))


(defn save-egg [filename {::e/keys [state col-widths row-heights]}]
  (let [data (-> state
                 (update ::e/col-widths merge col-widths)
                 (update ::e/row-heights merge row-heights))]
   (io/save-egg data filename)))


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


(defn- compile [code aliases cell-id] ;;TODO implement recompile-all based on this and on graph/advance
  (let [string-code (if (string? code) code (pr-str code))
        parsed      (if (string? code)
                      (read-string code)
                      code)]
   (try
     (let [ast (analyze/analyze parsed {:aliases (parse-aliases aliases)})]
       {:cell     cell-id
        :raw-code string-code
        :inputs   (map keyword (analyze/cell-refs ast))
        :code     (analyze/compile ast)})
     (catch Exception e
       {:cell     cell-id
        :raw-code string-code
        :inputs   []
        :error    e}))))


(defn- recompile-all [{::e/keys [graph aliases] :as state}]
  (let [function-cells (graph/functions graph)
        compiled       (map #(compile (graph/raw-code graph %) aliases %) function-cells)]
    (update state ::e/graph graph/advance {} compiled)))


(defn require-aliases! [aliases-text]
  (doseq [ns (map second (parse-aliases aliases-text))]
    (println "Requiring" ns)
    (try (require (symbol ns))
         (catch Exception _))))


(defn set-aliases! [state-atom aliases]
  (require-aliases! aliases)
  (swap! state-atom
         #(-> %
              (assoc ::e/aliases aliases)
              recompile-all)))


(defn add-libs! [deps]
  ;; (prn 'in-future)
  ;; (prn
  ;;  (take-while
  ;;   (complement nil?)
  ;;   (iterate #(.getParent %)
  ;;            (.getContextClassLoader (Thread/currentThread)))))
  (doseq [[lib coord] deps]
    (print "Adding lib:" (pr-str lib coord) " ")
    (let [res (try (deps/add-lib lib coord)
                   (catch Exception e
                     (.printStackTrace e)
                     (throw e)))]
      (println (if res "[ADDED]" "[SKIPPED]")))))


(defn set-deps! [state-atom deps]
  (add-libs! (edn/read-string deps))
  (swap! state-atom
         #(-> %
              (assoc ::e/deps deps)
              recompile-all)))


(defn load-egg [filename {:keys [state-atom grid]}]
  (let [{::e/keys [aliases deps col-widths row-heights] :as egg} (io/load-egg filename)]
    (add-libs! (edn/read-string deps))
    (require-aliases! aliases)
    (reset! state-atom
            (-> egg
                recompile-all
                (update ::e/graph rakk/recalc)))
    (ss/invoke-later
     (table/set-column-widths grid col-widths)
     (table/set-row-heights grid row-heights)))
  nil)


(defn- update-max-row-col [state [row col]]
  (-> state
      (update ::e/max-row (fnil max 0) row)
      (update ::e/max-col (fnil max 0) col)))


(defn- set-function-at! [state-atom cell-id [row col] value]
  (let [compiled (compile value (::e/aliases @state-atom) cell-id)]
    (swap! state-atom
           #(-> %
                (update-max-row-col [row col])
                (update ::e/graph graph/advance {} [compiled])))))


(defn set-cell-at! [state-atom [row col] value]
  (when value
    (let [cell-id (graph/coords->id row col)]
      (if (str/starts-with? value "(")
        (set-function-at! state-atom cell-id [row col] value)
        (swap! state-atom
               #(-> %
                    (update-max-row-col [row col])
                    (update ::e/graph graph/advance {cell-id (input->value value)} [])))))))


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


(defn get-dimensions [state-atom]
  (let [s @state-atom]
    [(or (::e/max-row s) 0)
     (or (::e/max-col s) 0)]))


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


(defn split-result [state-atom {:keys [value dynamic direction] :as opts}]
  (let [{:keys [original-value cell-id]} value
        aliases                          (::e/aliases @state-atom)
        cell-ids                         (take (count original-value)
                                               (iterate #(graph/cell-in-direction % direction)
                                                        (graph/cell-in-direction cell-id direction)))
        max-cell                         (graph/id->coords (last cell-ids))]
    (if dynamic
      (let [cell-id (-> cell-id name symbol)]
        (swap! state-atom
               #(-> %
                    (update-max-row-col max-cell)
                    (update ::e/graph graph/advance {}
                            (->> cell-ids
                                 (map-indexed (fn [idx dest-cell]
                                                (compile `(~'nth ~cell-id ~idx) aliases dest-cell))))))))
      (swap! state-atom
             #(-> %
                  (update-max-row-col max-cell)
                  (update ::e/graph graph/advance (zipmap cell-ids original-value) []))))))


;;(map fs/base-name (fs/list-dir "/Users/sideris/Downloads/pub/textures"))
;;(map (memfn getAbsolutePath) (fs/list-dir "/Users/sideris/Downloads/icons/png/"))
