(ns eggshell.gui.table
  (:require [seesaw.core :as ss]))

(defn selected-cell [^javax.swing.JTable table]
  [(.getSelectedRow table)
   (.getSelectedColumn table)])

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
