(ns eggshell.gui.table
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [seesaw.cursor :as cursor]
            [seesaw.meta :as meta]
            [eggshell :as e]
            [eggshell.util :as util]))


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


(defn visible-columns [^javax.swing.JTable table]
  (let [rect    (.getVisibleRect table)
        min-col (.columnAtPoint table (.getLocation rect))
        _       (.translate rect (.-width rect) 0)
        max-col (.columnAtPoint table (.getLocation rect))]
    [min-col max-col]))


(defn cell-rect [^javax.swing.JTable table [row col]]
  (.getCellRect table row col false))


(defn cell-at-point [^javax.swing.JTable table point]
  (cond (instance? java.awt.Point point)
        [(.rowAtPoint table point)
         (.columnAtPoint table point)]

        ;;TODO: maybe support [x y]?

        :else
        nil))

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


(defn column-width [^javax.swing.JTable table col]
  (some-> table .getColumnModel (.getColumn col) .getWidth))


(defn row-height [^javax.swing.JTable table row]
  (.getRowHeight table row))


(defn stop-editing! [^javax.swing.JTable table]
  (when-let [editor (.getCellEditor table)]
    (if (.getCellEditorValue editor)
      (.stopCellEditing editor)
      (.cancelCellEditing editor))))


(defn save-selection [^javax.swing.JTable table fun]
  (let [selection (selected-cell table)]
    (fun)
    (when selection (set-selection! table selection))))


(defn header-label [header]
  (doto (ss/text)
    (ss/config! :halign :center
                :background "#e9e9e9")
    (.setOpaque true)
    (.setBorder (border/line-border :color :lightgrey :bottom 1 :right 1))
    (.setForeground (.getForeground header))
    (.setFont (.getFont header))
    (.setDoubleBuffered true)))


;;;;;;;; Row header ;;;;;;;;


(defn row-header-renderer [^javax.swing.JTable table]
  (let [label (header-label (.getTableHeader table))]
    (proxy [javax.swing.table.DefaultTableCellRenderer] []
      (getTableCellRendererComponent [_ value is-selected has-focus row col]
        (ss/config! label
                    :text (str value)
                    :foreground "#6e5e5e"
                    :background (if (= row (first (selected-cell table))) :lightgreen "#e9e9e9")))))) ;;TODO extract color


(defn bottom-edge-rect [r]
  (java.awt.Rectangle. (.-x r) (+ (.-height r) (.-y r) -4)
                       (.-width r) 7))


(defn- row-header-pointer-handler [row-index mouse-event]
  (let [table         (.getSource mouse-event)
        root          (javax.swing.SwingUtilities/getRoot table)
        viz-rows      (apply range (visible-rows table))
        rects         (zipmap (map #(bottom-edge-rect (cell-rect table [% 0])) viz-rows)
                              viz-rows)
        point         (.getPoint mouse-event)
        matching-rect (first (filter #(.contains % point) (keys rects)))
        matching-row  (get rects matching-rect)]

    ;;for some reason setting the cursor on table does not work
    (if matching-rect
      (do
        (reset! row-index matching-row)
        (.setCursor root (cursor/cursor :s-resize)))
      (do
        (reset! row-index nil)
        (.setCursor root (cursor/cursor :default))))))


(defn set-column-width [table col width]
  (-> table .getColumnModel (.getColumn col) (.setPreferredWidth width))
  (let [widths (meta/get-meta table ::e/col-widths)]
    (meta/put-meta! table ::e/col-widths (assoc widths col width))))


(defn set-column-widths [table pairs]
  (doseq [[col w] pairs]
    (set-column-width table col w)))


(defn- set-row-height [table row height]
  (.setRowHeight table row height)
  (.setRowHeight (meta/get-meta table ::e/row-header) row height)
  (let [heights (meta/get-meta table ::e/row-heights)]
    (meta/put-meta! table ::e/row-heights (assoc heights row height))))


(defn set-row-heights [table pairs]
  (let [header (meta/get-meta table ::e/row-header)]
    (doseq [[row h] pairs]
      (.setRowHeight table row h)
      (.setRowHeight header row h))))


(defn- optimal-row-height [table row]
  (let [heights (for [col (range (min 1000 (.getColumnCount table)))] ;;TODO limit this to max occupied column - don't scan the whole table
                  (let [renderer  (.getCellRenderer table row col)
                        component (.prepareRenderer table renderer row col)]
                    (+ (-> component .getPreferredSize .height)
                       (-> table .getIntercellSpacing .height))))]
    (apply max (cons 20 heights)))) ;;TODO extract min row height


(def fit-row-to-data-monitor (atom nil))
(defn- fit-row-to-data [table row]
  ;;TODO warn user that this may be late
  (util/cfuture
   (util/unspam
    fit-row-to-data-monitor
    (let [optimal-height (optimal-row-height table row)]
      (ss/invoke-later (set-row-height table row optimal-height))))))


(defn- fit-all-rows-to-data [table row-header]
  (util/cfuture
   (util/unspam
    fit-row-to-data-monitor
    (let [optimal-heights (doall
                           (map (partial optimal-row-height table)
                                (range (min 100 (.getRowCount table)))))]
      (ss/invoke-later (doseq [[row h] (map vector (range) optimal-heights)]
                         (set-row-height table row h)))))))


(def fit-column-to-data-monitor (atom nil))

(defn- optimal-column-width [table col]
  (let [widths (for [row (range (min 1000 (.getRowCount table)))] ;;TODO limit this to max occupied column - don't scan the whole table
                 (let [renderer  (.getCellRenderer table row col)
                       component (.prepareRenderer table renderer row col)]
                   (+ (-> component .getPreferredSize .width)
                      (-> table .getIntercellSpacing .width))))]
    (apply max (cons 60 widths)))) ;;TODO extract min column width


(defn- fit-column-to-data [table col]
  (util/cfuture
   (util/unspam
    fit-column-to-data-monitor
    (let [optimal-width (optimal-column-width table col)]
      (ss/invoke-later (set-column-width table col optimal-width))))))


(defn- fit-all-columns-to-data [table]
  (util/cfuture
   (util/unspam
    fit-column-to-data-monitor
    (let [optimal-widths (doall
                          (map (partial optimal-column-width table)
                               (range (min 100 (.getColumnCount table)))))]
      (ss/invoke-later (doseq [[col w] (map vector (range) optimal-widths)]
                         (set-column-width table col w)))))))


(defn apply-row-heights [table]
  (let [v (meta/get-meta table ::e/row-header)]
    (doseq [r (range (.getRowCount table))]
      (.setRowHeight table r (max 1 (.getRowHeight v r))))))


(defn apply-column-widths [table]
  (set-column-widths table (meta/get-meta table ::e/col-widths)))


(defn row-header [^javax.swing.JTable table]
  (let [dragged-start (atom nil)
        row-index     (atom nil)
        header
        (doto (ss/table
               :enabled? false
               :model
               (proxy [javax.swing.table.DefaultTableModel] []
                 (getColumnCount [] 1)
                 (getRowCount [] (.getRowCount table))
                 (isCellEditable [row col] false)
                 (getColumnName [col] "row")
                 (getValueAt [row col] row)
                 (setValueAt [value row col])
                 (getColumnClass [^Integer c] Object))
               :listen [:mouse-moved    (partial row-header-pointer-handler row-index)
                        :mouse-exited (fn [_]
                                        (let [root (javax.swing.SwingUtilities/getRoot table)]
                                          (.setCursor root (cursor/cursor :default))))
                        :mouse-pressed  (fn [e]
                                          (if (and @row-index (= 2 (.getClickCount e)))
                                            (if (.isShiftDown e)
                                              (fit-all-rows-to-data table (.getSource e))
                                              (fit-row-to-data table (.getSource e) @row-index))
                                            (reset! dragged-start (.getY e))))
                        :mouse-released (fn [e] (reset! dragged-start nil))
                        :mouse-dragged  (fn [e]
                                          (when-let [ds @dragged-start]
                                            (when-let [row @row-index]
                                              (let [d          (- (.getY e) ds)
                                                    new-height (max 20 (+ (row-height table row) d))]
                                                (set-row-height table row new-height)
                                                (reset! dragged-start (.getY e))))))])
          (.setRowHeight (.getRowHeight table))
          (.setDefaultRenderer Object (row-header-renderer table))
          ;;(.setPreferredSize (java.awt.Dimension. 50 450))
          )]
    (meta/put-meta! table ::e/row-header header)
    header))


;;;;;;;; Column header ;;;;;;;;


(defn column-header-renderer [^javax.swing.JTable table]
  (let [label (header-label (.getTableHeader table))]
    (proxy [javax.swing.table.DefaultTableCellRenderer] []
      (getTableCellRendererComponent [_ value is-selected has-focus row col]
        (ss/config! label
                    :text (str value)
                    :foreground "#6e5e5e"
                    :background (if (= col (second (selected-cell table))) :lightgreen "#e9e9e9"))))))


(defn right-edge-rect [r]
  (java.awt.Rectangle. (+ (.-x r) (.-width r) -4) (.-y r)
                       7 (.-height r)))


(defn- column-header-pointer-handler [table col-index mouse-event]
  (let [header        (.getSource mouse-event)
        root          (javax.swing.SwingUtilities/getRoot table)
        viz-cols      (apply range (visible-columns table))
        rects         (zipmap (map #(right-edge-rect (.getHeaderRect header %)) viz-cols)
                              viz-cols)
        point         (.getPoint mouse-event)
        matching-rect (first (filter #(.contains % point) (keys rects)))
        matching-col  (get rects matching-rect)]

    ;;for some reason setting the cursor on table does not work
    (if matching-rect
      (do
        (reset! col-index matching-col)
        (.setCursor root (cursor/cursor :e-resize)))
      (do
        (reset! col-index nil)
        (.setCursor root (cursor/cursor :default))))))


(defn config-column-resize! [table]
  (let [col-index (atom nil)]
    (ss/listen (.getTableHeader table)
               :mouse-moved (partial column-header-pointer-handler table col-index)
               :mouse-exited (fn [_]
                               (let [root (javax.swing.SwingUtilities/getRoot table)]
                                 (.setCursor root (cursor/cursor :default))))
               :mouse-released (fn [e] (when-let [col @col-index]
                                         (let [widths (meta/get-meta table ::e/col-widths)]
                                           (meta/put-meta! table ::e/col-widths (assoc widths col (column-width table col))))))
               :mouse-pressed (fn [e]
                                (when (and @col-index (= 2 (.getClickCount e)))
                                  (if (.isShiftDown e)
                                    (fit-all-columns-to-data table)
                                    (fit-column-to-data table @col-index)))))))
