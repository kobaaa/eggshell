(ns eggshell.gui
  (:require [seesaw.core :as ss]
            [seesaw.font :as font]
            [seesaw.color :as color]
            [seesaw.keymap :as keymap]
            [seesaw.chooser :as chooser]
            [seesaw.dev :as dev]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [eggshell.controller :as controller]
            [eggshell.gui.table :as table]
            [clojure.string :as str]
            [clojure.edn :as edn]))


(def mono-font (font/font :name "Monaco" :size 12))
(def text-font (font/default-font "TextField.font"))


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
  (if (graph/function? g cell)
    (graph/raw-code g cell)
    (if-let [v (graph/value g cell)]
      (cond (string? v)
            v
            :else
            (pr-str v))
      nil)))


(defn cell-editor [g]
  (let [text-field (ss/text)]
    (proxy [javax.swing.DefaultCellEditor] [text-field]
      (getTableCellEditorComponent [table value is-selected row col]
        (let [cell      (graph/coords->id row (dec col))
              function? (graph/function? g cell)]
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
        (graph/idx->column (dec col))))

    (getValueAt [row col]
      (if (zero? col)
        row
        (let [cell-id (graph/coords->id row (dec col))]
          (or (render-value (graph/value @g (keyword cell-id)))
              ""))))

    (setValueAt [value row col]
      (controller/set-cell-at! g [row (dec col)] value))

    (getColumnClass [^Integer c]
      (proxy-super getColumnClass c)
      Object)))


(defn table [graph-atom model]
  (doto (ss/table :id :grid
                  :auto-resize :off
                  :show-grid? true
                  :model model)
    (.setDefaultEditor Object (cell-editor graph-atom))
    (.setCellSelectionEnabled true)
    (.setGridColor (color/color "lightgray"))
    (.setRowHeight 20)))


(defn code-editor []
  (ss/text :id :code-editor
           :font mono-font
           ;;:multi-line? true
           ))


(defn wire! [frame graph-atom table-model]
  (let [{:keys [code-editor grid]} (ss/group-by-id frame)]

    ;;update table when grid graph changes
    (add-watch graph-atom :kk (fn [_ _ _ _] (.fireTableDataChanged table-model)))

    ;;listen for cell selection changes to update code editor
    (table/listen-selection
     grid
     (fn [e]
       (let [[row col] (table/selected-cell grid)
             cell      (graph/coords->id row (dec col))]
         (ss/config! code-editor :text
                     (editable-value @graph-atom cell)))))

    ;;listen to ENTER to update cell being edited
    (keymap/map-key code-editor "ENTER"
                    (fn [_]
                      (let [[row col] (table/selected-cell grid)]
                        (controller/set-cell-at! graph-atom
                                                 [row (dec col)]
                                                 (ss/value code-editor)))))))


(defn- toolbar []
  (ss/flow-panel
   :align :left
   :items
   [(ss/button :text "Load"
               :listen
               [:action
                (fn [_]
                  (when-let [file (chooser/choose-file)]
                    (controller/load-egg file)))])
    (ss/button :text "Save"
               :listen
               [:action
                (fn [_]
                  (when-let [file (chooser/choose-file :type :save)]
                    (controller/save-egg file)))])]))


(defn grid-frame [graph-atom]
  (let [model (table-model graph-atom)
        frame (ss/frame :title "eggshell"
                        :content (ss/border-panel
                                  :north (toolbar)
                                  :center
                                  (ss/border-panel
                                   :north  (code-editor)
                                   :center (ss/scrollable (table graph-atom model))))
                        :on-close :dispose)]
    (wire! frame graph-atom model)
    (ss/invoke-later (-> frame ss/pack! ss/show!))))


;;(grid-frame graph/graph-atom)
;;(eggshell.controller/load-egg "test-resources/first.egg")
