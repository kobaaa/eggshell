(ns seesaw.gui
  (:require [seesaw.core :as ss]
            [eggshell.grid :as grid]
            [eggshell.analyze :as analyze]
            [clojure.string :as str]
            [clojure.edn :as edn]))


(defn set-cell-at! [g [row col] value]
  (let [cell-id (grid/coords->id row col)]
    (prn :cell cell-id)
    (if (str/starts-with? value "(")
      (let [code (edn/read-string value)
            ast  (analyze/analyze code)]
        (swap! g grid/advance {} [{:cell   cell-id
                                   :inputs (map keyword (analyze/cell-refs ast))
                                   :code   (analyze/compile ast)}]))
      (swap! g grid/advance {cell-id (edn/read-string value)} []))))


(defn render-value [x]
  (cond (seq? x)
        (pr-str (doall x))
        (nil? x)
        ""
        :else
        (pr-str x)))


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
                      (.setCellSelectionEnabled true)))
                   :on-close :dispose)
         ss/pack!
         ss/show!))))

;;(grid-frame grid/graph-atom)
