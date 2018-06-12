(ns seesaw.gui
  (:require [seesaw.core :as ss]
            [seesaw.font :as font]
            [eggshell.grid :as grid]
            [eggshell.analyze :as analyze]
            [clojure.string :as str]
            [clojure.edn :as edn]))


(def mono-font (font/font :name "Monaco" :size 12))
(def text-font (font/default-font "TextField.font"))

;;TODO error handling
(defn input->value [s]
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
  (let [cell-id (grid/coords->id row col)]
    (if (str/starts-with? value "(")
      (let [code (read-string value)
            ast  (analyze/analyze code)]
        (swap! g grid/advance {} [{:cell     cell-id
                                   :inputs   (map keyword (analyze/cell-refs ast))
                                   :raw-code value
                                   :code     (analyze/compile ast)}]))
      (swap! g grid/advance {cell-id (input->value value)} []))))


(defn render-value [x]
  (cond (seq? x)
        (pr-str (doall x))
        (string? x)
        x
        (nil? x)
        ""
        :else
        (pr-str x)))


(defn editable-value [g cell]
  (if (grid/function? g cell)
    (grid/raw-code g cell)
    (if-let [v (grid/value g cell)]
      (cond (string? v)
            v
            :else
            (pr-str v))
      nil)))


(defn cell-editor [g]
  (let [text-field (ss/text)]
    (proxy [javax.swing.DefaultCellEditor] [text-field]
      (getTableCellEditorComponent [table value is-selected row col]
        (let [cell      (grid/coords->id row (dec col))
              function? (grid/function? g cell)]
          (doto text-field
            ;;(.setFont (if function? mono-font text-font))
            (.setFont mono-font)
            (.setText (editable-value @g cell))))))))


(defn table-model [g]
  (proxy [javax.swing.table.DefaultTableModel] []

    (getColumnCount [] 50 ;;702
      ) ;; excel has 16384

    (getRowCount [] 1048576)

    (isCellEditable [row col]
      (not= col 0))

    (getColumnName [col]
      (if (zero? col)
        ""
        (grid/idx->column (dec col))))

    (getValueAt [row col]
      (if (zero? col)
        row
        (let [cell-id (grid/coords->id row (dec col))]
          (or (render-value (grid/value @g (keyword cell-id)))
              ""))))

    (setValueAt [value row col]
      (set-cell-at! g [row (dec col)] value))

    (getColumnClass [^Integer c]
      (proxy-super getColumnClass c)
      Object)))

(defn grid-frame [graph-atom]
  (let [model (table-model graph-atom)]
    (add-watch graph-atom :kk (fn [_ _ _ _] (.fireTableDataChanged model)))
    (ss/invoke-later
     (-> (ss/frame :title "eggshell"
                   :content
                   (ss/scrollable
                    (doto (ss/table :auto-resize :off
                                    :show-grid? true
                                    :model model)
                      (.setDefaultEditor Object (cell-editor graph-atom))
                      (.setCellSelectionEnabled true)
                      (.setRowHeight 20)))
                   :on-close :dispose)
         ss/pack!
         ss/show!))))

;;(grid-frame grid/graph-atom)
