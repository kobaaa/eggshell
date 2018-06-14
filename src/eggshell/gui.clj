(ns eggshell.gui
  (:require [seesaw.core :as ss]
            [seesaw.font :as font]
            [seesaw.color :as color]
            [seesaw.keymap :as keymap]
            [seesaw.dev :as dev]
            [eggshell.graph :as graph]
            [eggshell.analyze :as analyze]
            [eggshell.gui.table :as table]
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
  (let [cell-id (graph/coords->id row col)]
    (if (str/starts-with? value "(")
      (let [code (read-string value)
            ast  (analyze/analyze code)]
        (swap! g graph/advance {} [{:cell     cell-id
                                    :inputs   (map keyword (analyze/cell-refs ast))
                                    :raw-code value
                                    :code     (analyze/compile ast)}]))
      (swap! g graph/advance {cell-id (input->value value)} []))))


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
      (set-cell-at! g [row (dec col)] value))

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
                    (fn [_] (prn "You pressed enter!")
                      (let [[row col] (table/selected-cell grid)]
                        (set-cell-at! graph-atom
                                      [row (dec col)]
                                      (ss/value code-editor)))))))


(defn grid-frame [graph-atom]
  (let [model (table-model graph-atom)
        frame (ss/frame :title "eggshell"
                        :content (ss/border-panel
                                  :north  (code-editor)
                                  :center (ss/scrollable (table graph-atom model)))
                        :on-close :dispose)]
    (wire! frame graph-atom model)
    (ss/invoke-later (-> frame ss/pack! ss/show!))))


;;(grid-frame graph/graph-atom)
;;(eggshell.controller/load-eggshell "test-resources/first.egg")
