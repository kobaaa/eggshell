(ns eggshell.gui
  (:require [seesaw.core :as ss]
            [seesaw.color :as color]
            [seesaw.keymap :as keymap]
            [seesaw.chooser :as chooser]
            [seesaw.border :as border]
            [seesaw.color :as color]
            [seesaw.dev :as dev]
            [eggshell.graph :as graph]
            [rakk.core :as rakk]
            [eggshell.analyze :as analyze]
            [eggshell.controller :as controller]
            [eggshell.gui.table :as table]
            [eggshell.gui.code-editor :as code-editor]
            [eggshell.gui.defaults :as defaults]
            [eggshell.state :as state]
            [eggshell.util :as util]
            [clojure.repl :as repl]
            [clojure.string :as str]
            [clojure.edn :as edn]))


(defn render-value [x]
  (cond (= x :rakk/error)
        "ERROR!"
        (seq? x)
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

          (ss/config! text-field
            ;;(.setFont (if function? mono-font text-font))
            :font defaults/mono-font
            :text (editable-value @g cell)))))))


(defn- value-at [g row col]
  (if (zero? col)
    {:render-value (str row)}
    (let [cell-id (keyword (graph/coords->id row (dec col)))]
      (if-let [v (graph/value g cell-id)]
        (merge
         {:original-value v
          :render-value   (render-value v)
          :cell-id        cell-id}
         (rakk/error-info g cell-id))
        ""))))


(defn- render [component g
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



(defn cell-renderer [g]
  (proxy [javax.swing.table.DefaultTableCellRenderer] []
    (getTableCellRendererComponent [table value is-selected has-focus row col]
      (let [c (proxy-super getTableCellRendererComponent table value is-selected has-focus row col)]
        (@#'render c g value is-selected)))))


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

    (getValueAt [row col] (value-at @g row col))

    (setValueAt [value row col]
      (future
        (controller/set-cell-at! g [row (dec col)] (if (= value "") nil value))))

    (getColumnClass [^Integer c]
      (proxy-super getColumnClass c)
      Object)))


(defn table [graph-atom model]
  (doto (ss/table :id :grid
                  :auto-resize :off
                  :show-grid? true
                  :model model)
    (.setDefaultRenderer Object (cell-renderer graph-atom))
    (.setDefaultEditor Object (cell-editor graph-atom))
    (.setCellSelectionEnabled true)
    (.setGridColor (color/color "lightgray"))
    (.setRowHeight 20)))


(defn- status-line-text [{:keys [graph cell-id error?]}]
  (str
   (name cell-id)
   (when (graph/function? graph cell-id) ", function")
   (when error? " - ERROR! click to expand")))


(defn- status-area []
  (ss/border-panel
   :id     :status-area
   :north  (ss/label :id :status-line :text "OK" :border 4 :foreground :gray)
   :center (ss/horizontal-panel
            :id :error-area
            :visible? false
            :border 3
            :preferred-size [100 :by 200]
            :items
            [(ss/scrollable
              (ss/text :id :error-text-area :multi-line? true :font defaults/mono-font))])))


(defn- error-trace-text [error]
  (str
   (when (some-> error ex-data :rakk/secondary-error some?)
     (str "Some upstream cells contain errors: "
          (str/join ", " (->> error ex-data :rakk/upstream-errors (map (comp name :node)) sort))
          "\n\n"))
   (str/replace (util/with-err-str (repl/pst error))
                "\t" "   ")))


(defn update-status-area! [status-area error-text-area grid graph-atom]
  (let [[row col] (table/selected-cell grid)]
    (if (and row col)
      (let [cell-id                                    (graph/coords->id row (dec col))
            {:rakk/keys [error? error] :as error-info} (rakk/error-info @graph-atom cell-id)]
        (ss/value! status-area
                   {:status-line     (status-line-text {:graph   @state/graph-atom
                                                        :cell-id cell-id
                                                        :error?  error})
                    :error-text-area (if-not error? "No errors" (error-trace-text error))})
        (ss/scroll! error-text-area :to :top))
      (ss/value! status-area
                 {:status-line     "No selection"
                  :error-text-area "No errors"}))))


(defn wire! [frame graph-atom table-model]
  (let [{:keys [code-editor grid load-button save-button status-area status-line error-area error-text-area]}
        (ss/group-by-id frame)]

    ;;update table when grid graph changes
    (add-watch graph-atom :kk (fn [_ _ _ _] (ss/invoke-now (.fireTableDataChanged table-model))))

    ;;listen for cell selection changes to update code editor
    (table/listen-selection
     grid
     (fn [_]
       (let [selected (table/selected-cell grid)]
         (if-not selected
           (ss/config! code-editor
                       :text      ""
                       :editable? false)
           (let [[row col] selected
                 cell      (graph/coords->id row (dec col))]
             (ss/config! code-editor
                         :text      (editable-value @graph-atom cell)
                         :editable? true))))))

    ;;listen to ENTER to update cell being edited
    (keymap/map-key code-editor "ENTER"
                    (fn [_]
                      (let [[row col] (table/selected-cell grid)]
                        (controller/set-cell-at! graph-atom
                                                 [row (dec col)]
                                                 (ss/value code-editor))
                        (table/set-selection! grid [row col])))
                    :scope :self)


    (keymap/map-key code-editor "control ENTER"
                    (fn [_] (code-editor/insert-new-line! code-editor)))


    ;;wire up toolbar buttons

    (ss/listen load-button :action
               (fn [_]
                 (when-let [file (chooser/choose-file)]
                   (controller/load-egg file {:graph-atom state/graph-atom
                                              :grid       grid}))))
    (ss/listen save-button :action
               (fn [_]
                 (when-let [file (chooser/choose-file :type :save)]
                   (controller/save-egg file {:graph         @state/graph-atom
                                              :column-widths (table/column-widths grid)}))))

    ;;wire up status area
    (table/listen-selection grid (fn [_] (update-status-area! status-area error-text-area grid graph-atom)))

    (ss/listen status-line :mouse-clicked
               (fn [_]
                 (ss/config! error-area
                             :visible? (not (ss/config error-area :visible?))
                             :preferred-size [(.getWidth error-area) :by 200])))))


(defn- toolbar []
  (ss/flow-panel
   :align :left
   :items
   [(ss/button :text "Load" :id :load-button)
    (ss/button :text "Save" :id :save-button)]))


(defn grid-frame [graph-atom]
  (let [model (table-model graph-atom)
        frame (ss/frame :title "eggshell"
                        :content (ss/border-panel
                                  :north (toolbar)
                                  :center
                                  (ss/border-panel
                                   :north  (code-editor/code-editor)
                                   :center (ss/scrollable (table graph-atom model)))
                                  :south (status-area))
                        :on-close :dispose)]
    (wire! frame graph-atom model)
    (ss/invoke-later (-> frame ss/pack! ss/show!))))


(defn frames []
  (java.awt.Frame/getFrames))

;;(grid-frame state/graph-atom)
;;(eggshell.gui/grid-frame eggshell.state/graph-atom)
;;(eggshell.controller/load-egg "test-resources/first.egg")
;;(def tt (ss/select (last (frames)) [:#grid]))
