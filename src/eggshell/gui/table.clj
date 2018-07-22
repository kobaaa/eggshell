(ns eggshell.gui.table
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [seesaw.cursor :as cursor]))


(defn selected-cell [^javax.swing.JTable table]
  (let [selected [(.getSelectedRow table) (.getSelectedColumn table)]]
    (when-not (= [-1 -1] selected) selected)))


(defn selected-cells [^javax.swing.JTable table]
  {:selected-rows    (set (.getSelectedRows table))
   :selected-columns (set (.getSelectedColumns table))})


(defn set-selection! [^javax.swing.JTable table [row col]]
  (.setRowSelectionInterval table row row)
  (.setColumnSelectionInterval table col col))


(defn visible-rows [^javax.swing.JTable table]
  (let [rect    (.getVisibleRect table)
        min-row (.rowAtPoint table (.getLocation rect))
        _       (.translate rect 0 (.-height rect))
        max-row (.rowAtPoint table (.getLocation rect))]
    [min-row max-row]))


(defn cell-rect [^javax.swing.JTable table [row col]]
  (.getCellRect table row col false))


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


(defn row-header-renderer [^javax.swing.JTable table]
  (let [header (.getTableHeader table)
        label  (doto (ss/text)
                 (ss/config! :halign :center
                             :background "#e9e9e9")
                 (.setOpaque true)
                 (.setBorder (border/line-border :color :lightgrey :bottom 1 :right 1))
                 (.setForeground (.getForeground header))
                 (.setFont (.getFont header))
                 (.setDoubleBuffered true))]
    (proxy [javax.swing.table.DefaultTableCellRenderer] []
      (getTableCellRendererComponent [_ value is-selected has-focus row col]
        (ss/config! label
                    :text (str value)
                    :background (if (= row (first (selected-cell table))) :lightgreen "#e9e9e9"))
        ;; (.setPreferredSize nil)
        ;; (.setPreferredSize
        ;;  (java.awt.Dimension. (-> label .getPreferredSize .getWidth) (.getRowHeight table row)))
        ))))

(defn bottom-edge-rect [r]
  (java.awt.Rectangle. (.-x r) (- (.-y r) 2)
                       (.-width r) 4))


(defn- row-header-pointer-handler [mouse-event]
  ;;(prn (str mouse-event))
  (let [table (.getSource mouse-event)
        rects (map #(bottom-edge-rect (cell-rect table [% 0]))
                   (apply range (visible-rows table)))
        point (.getPoint mouse-event)]
    (comment
     (if (some #(.contains % point) rects)
       (do (prn 'CONTAINS!)
           (.setCursor table (cursor/cursor :move)))
       (do (prn 'NOT!)
           (.setCursor table (cursor/cursor :default)))))))


(defn row-header [^javax.swing.JTable table]
  (doto (ss/table
         :enabled? false
         :cursor :move
         :model
         (proxy [javax.swing.table.DefaultTableModel] []
           (getColumnCount [] 1)
           (getRowCount [] (.getRowCount table))
           (isCellEditable [row col] false)
           (getColumnName [col] "row")
           (getValueAt [row col] row)
           (setValueAt [value row col])
           (getColumnClass [^Integer c] Object))
         :listen [:mouse-moved row-header-pointer-handler])
    (.setRowHeight (.getRowHeight table))
    (.setDefaultRenderer Object (row-header-renderer table))
    ;;(.setPreferredSize (java.awt.Dimension. 50 450))
    ))
