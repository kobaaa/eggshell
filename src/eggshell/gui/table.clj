(ns eggshell.gui.table
  (:require [seesaw.core :as ss]))


(defn selected-cell [^javax.swing.JTable table]
  (let [selected [(.getSelectedRow table) (.getSelectedColumn table)]]
    (when-not (= [-1 -1] selected) selected)))


(defn set-selection! [^javax.swing.JTable table [row col]]
  (.setRowSelectionInterval table row row)
  (.setColumnSelectionInterval table col col))


(defn listen-selection [^javax.swing.JTable table f]
  ;;this is to detect column selection changes
  (-> table
      .getColumnModel
      (.addColumnModelListener
       (proxy [javax.swing.event.TableColumnModelListener] []
         (columnAdded [e])
         (columnMarginChanged [e])
         (columnMoved [e])
         (columnRemoved [e])

         (columnSelectionChanged [e]
           (f e)))))

  ;;this one only fires when the row changes
  (ss/listen table :selection f))


(defn column-widths [^javax.swing.JTable table]
  (let [model (.getColumnModel table)]
    (doall
     (for [idx (range (.getColumnCount model))]
       (let [col (.getColumn model idx)]
         (.getWidth col))))))


(defn stop-editing! [^javax.swing.JTable table]
  (when-let [editor (.getCellEditor table)]
    (if (.getCellEditorValue editor)
      (.stopCellEditing editor)
      (.cancelCellEditing editor))))


(defn save-selection [^javax.swing.JTable table fun]
  (let [selection (selected-cell table)]
    (fun)
    (when selection (set-selection! table selection))))
