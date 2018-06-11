(ns seesaw.gui
  (:require [seesaw.core :as ss]
            [eggshell.grid :as grid]))



(defn table-model [g]
  (proxy [javax.swing.table.DefaultTableModel] []

    (getColumnCount [] 50 ;;702
      ) ;; excel has 16384

    (getRowCount [] 1048576)

    (isCellEditable [row col] false)

    (getColumnName [col]
      (if (zero? col)
        ""
        (grid/idx->column (dec col))))

    (getValueAt [row col]
      (if (zero? col)
        row
        (let [cell-id (grid/coords->id row (dec col))]
          (or (grid/value @g (keyword cell-id))
              ""))))

    (setValueAt [value row col]
      )

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
