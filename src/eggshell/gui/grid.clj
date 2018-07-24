(ns eggshell.gui.grid
  (:require [seesaw.core :as ss]
            [seesaw.color :as color]
            [eggshell.gui.defaults :as defaults]
            [eggshell.graph :as graph]
            [eggshell.util :as util :refer [cfuture]]
            [eggshell.gui.table :as table]
            [eggshell.layer :as layer]))


;;;;;;;; model and editing ;;;;;;;;


(defn cell-editor [editable-getter]
  (let [text-field (ss/text)]
    (proxy [javax.swing.DefaultCellEditor] [text-field]
      (getTableCellEditorComponent [table value is-selected row col]
        (ss/config! text-field
                    :border nil
                    :font defaults/mono-font
                    :text (editable-getter [row col])))
      (shouldSelectCell [_] true))))


(defn- render [component
               {:keys [original-value render-value cell-id :rakk/error? :rakk/error-type] :as value}
               is-selected?]
  (ss/config! component :halign (if error? :center :left))
  (doto component
    (.setText render-value)
    (.setForeground (color/color (if (= error-type :primary) :white :black)))
    (.setBackground (color/color (cond (= error-type :primary) defaults/primary-error-color
                                       (= error-type :secondary) defaults/secondary-error-color
                                       is-selected? defaults/selected-color
                                       :else :white)))))


(defn cell-renderer []
  (proxy [javax.swing.table.DefaultTableCellRenderer] []
    (getTableCellRendererComponent [table value is-selected has-focus row col]
      (let [c (proxy-super getTableCellRendererComponent table value is-selected has-focus row col)]
        (@#'render c value is-selected)))))


(defn table-model [cell-getter cell-setter]
  (proxy [javax.swing.table.DefaultTableModel] []

    (getColumnCount [] 50 ;;702
      ) ;; excel has 16384

    (getRowCount [] 1048576)

    (isCellEditable [row col]
      true)

    (getColumnName [col]
      (graph/idx->column col))

    (getValueAt [row col] (cell-getter [row col]))

    (setValueAt [value row col]
      (cfuture
        (cell-setter [row col] (if (= value "") nil value))))

    (getColumnClass [^Integer c]
      (proxy-super getColumnClass c)
      Object)))


;;;;;;;; glass pane (selection and focus rendering) ;;;;;;;;


(defn- convert-rect [source rect dest]
  (javax.swing.SwingUtilities/convertRectangle source rect dest))


(defn- translate-rect [rect dx dy]
  (let [rect2 (java.awt.Rectangle. rect)]
    (.translate rect2 dx dy)
    rect2))


(defn- grow-rect [rect d-width d-height]
  (let [rect2 (java.awt.Rectangle. rect)]
    (.setBounds rect2 (.-x rect) (.-y rect) (+ (.-width rect) d-width) (+ (.-height rect) d-height))
    rect2))


(defn- draw-focus-box [table root-pane g]
  (let [[row col] (table/selected-cell table)]
    (when (and row col)
      (let [cell-rect (convert-rect table (table/cell-rect table [row col]) root-pane)]
        (doto g
          (.setStroke (java.awt.BasicStroke. 2))
          (.setColor (color/color "#247247"))
          (.drawRect (dec (.-x cell-rect))
                     (dec (.-y cell-rect))
                     (+ (.-width cell-rect) 2)
                     (+ (.-height cell-rect) 2))
          (.fillRect (+ (.-x cell-rect) (.-width cell-rect) -2)
                     (+ (.-y cell-rect) (.-height cell-rect) -2)
                     5 5)
          (.setColor (color/color :white))
          (.setStroke (java.awt.BasicStroke. 1))
          (.drawLine (+ (.-x cell-rect) (.-width cell-rect))
                     (+ (.-y cell-rect) (.-height cell-rect) -3)
                     (+ (.-x cell-rect) (.-width cell-rect) 1)
                     (+ (.-y cell-rect) (.-height cell-rect) -3))
          (.drawLine (+ (.-x cell-rect) (.-width cell-rect) -3)
                     (+ (.-y cell-rect) (.-height cell-rect))
                     (+ (.-x cell-rect) (.-width cell-rect) -3)
                     (+ (.-y cell-rect) (.-height cell-rect) 1)))))))


(defn- draw-selections [table root-pane g]
  (let [{:keys [selected-rows selected-columns] :as selected} (table/selected-cells table)
        focused-cell                                          (table/selected-cell table)]
    (doseq [row selected-rows]
      (doseq [col selected-columns]
        (when-not (= focused-cell [row col])
          (let [cell-rect (convert-rect table (table/cell-rect table [row col]) root-pane)]
            (doto g
              (.setColor (color/color "#add8e6" 64))
              (.fill cell-rect))))))))


(defn glass-pane [root-pane table]
  (proxy [javax.swing.JComponent] []
    (paintComponent [g]
      (let [table-rect (convert-rect table (.getVisibleRect table) root-pane)]
        (.setClip g table-rect)
        (draw-selections table root-pane g)
        ;;be a bit more permissive because the focus box can appear outside the bounds of the table by a couple of pixels
        (.setClip g (-> table-rect
                        (translate-rect -2 -2)
                        (grow-rect 5 5)))
        (draw-focus-box table root-pane g)))))


(defn apply-row-heights [table scroll-pane]
  (let [v (-> scroll-pane .getRowHeader .getView)]
    (doseq [r (apply range (table/visible-rows v))]
      (.setRowHeight table r (.getRowHeight v r)))))


;;;;;;;; grid ;;;;;;;;


(defn make-grid [layer editable-getter]
  (let [table      (doto (ss/table
                          :id :grid
                          :auto-resize :off
                          :show-grid? true
                          :model (layer/to-model layer))
                     ;;(.putClientProperty "terminateEditOnFocusLost" true)
                     (.setDefaultRenderer Object (layer/to-cell-renderer layer))
                     (.setDefaultEditor Object (cell-editor editable-getter))
                     (.setCellSelectionEnabled true)
                     (.setGridColor (color/color "#cecece"))
                     (.setRowHeight 20)
                     table/config-column-resize-pointer!)
        scrollable (doto (ss/scrollable table :id :grid-scroll)
                     (.setRowHeaderView (table/row-header table)))]
    (doto (.getTableHeader table)
      (.setReorderingAllowed false)
      (.setDefaultRenderer (table/column-header-renderer table)))
    (-> scrollable .getRowHeader (.setPreferredSize (java.awt.Dimension. 60 450)))
    {:scroll-pane scrollable
     :table       table}))
