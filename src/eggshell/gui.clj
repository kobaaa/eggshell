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
            [eggshell.gui.aliases :as aliases]
            [eggshell.state :as state]
            [eggshell.util :as util]
            [clojure.repl :as repl]
            [clojure.string :as str]
            [clojure.edn :as edn]))


(defn cell-editor [editable-getter]
  (let [text-field (ss/text)]
    (proxy [javax.swing.DefaultCellEditor] [text-field]
      (getTableCellEditorComponent [table value is-selected row col]
        (ss/config! text-field
                    :font defaults/mono-font
                    :text (editable-getter [row (dec col)])))
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
      (not= col 0))

    (getColumnName [col]
      (if (zero? col)
        ""
        (graph/idx->column (dec col))))

    (getValueAt [row col] (cell-getter [row (dec col)]))

    (setValueAt [value row col]
      (future
        (cell-setter [row (dec col)] (if (= value "") nil value))))

    (getColumnClass [^Integer c]
      (proxy-super getColumnClass c)
      Object)))


(defn grid [model editable-getter]
  (doto (ss/table :id :grid
                  :auto-resize :off
                  :show-grid? true
                  :model model)
    ;;(.putClientProperty "terminateEditOnFocusLost" true)
    (.setDefaultRenderer Object (cell-renderer))
    (.setDefaultEditor Object (cell-editor editable-getter))
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


(defn update-status-area! [status-area error-text-area grid graph]
  (let [[row col] (table/selected-cell grid)]
    (if (and row col)
      (let [cell-id                                    (graph/coords->id row (dec col))
            {:rakk/keys [error? error] :as error-info} (rakk/error-info graph cell-id)]
        (ss/value! status-area
                   {:status-line     (status-line-text {:graph   graph
                                                        :cell-id cell-id
                                                        :error?  error})
                    :error-text-area (if-not error? "No errors" (error-trace-text error))})
        (ss/scroll! error-text-area :to :top))
      (ss/value! status-area
                 {:status-line     "No selection"
                  :error-text-area "No errors"}))))


(defn wire! [{:keys [frame state-atom table-model cell-setter editable-getter egg-loader]}]
  (let [{:keys [load-button save-button aliases-button
                code-editor grid
                status-area status-line error-area error-text-area]}
        (ss/group-by-id frame)
        graph (:graph @state-atom)]

    ;;update table when grid graph changes
    (add-watch state-atom :kk
               (fn [_ _ _ _]
                 (table/save-selection
                  grid
                  #(ss/invoke-now (.fireTableDataChanged table-model)))))

    ;;listen for cell selection changes to update code editor
    (table/listen-selection
     grid
     (fn [_]
       (let [selected (table/selected-cell grid)]
         (if-not selected
           (ss/config! code-editor
                       :text      ""
                       :editable? false)
           (let [[row col] selected]
             (ss/config! code-editor
                         :text      (editable-getter [row (dec col)])
                         :editable? true))))))


    (keymap/map-key grid "F2"
                    (fn [_]
                      (table/stop-editing! grid)
                      (ss/request-focus! code-editor)))


    (keymap/map-key code-editor "ESCAPE"
                    (fn [_]
                      (ss/request-focus! grid)))


    ;;listen to ENTER to update cell being edited
    (keymap/map-key code-editor "ENTER"
                    (fn [_]
                      (let [[row col] (table/selected-cell grid)]
                        (cell-setter [row (dec col)] (ss/value code-editor))
                        (table/set-selection! grid [row col])))
                    :scope :self)


    (keymap/map-key code-editor "control ENTER"
                    (fn [_] (code-editor/insert-new-line! code-editor)))


    ;;wire up toolbar buttons

    (ss/listen load-button :action
               (fn [_]
                 (when-let [file (chooser/choose-file)]
                   (egg-loader file grid))))

    (ss/listen save-button :action
               (fn [_]
                 (when-let [file (chooser/choose-file :type :save)]
                   (controller/save-egg file {:graph         graph
                                              :column-widths (table/column-widths grid)}))))
    (ss/listen aliases-button :action
               (fn [_]
                 (aliases/aliases-frame (:aliases @state-atom)
                                        {:parent frame
                                         :apply-fn (partial controller/set-aliases! state-atom)})))

    ;;wire up status area
    (table/listen-selection grid (fn [_] (update-status-area! status-area error-text-area grid (:graph @state-atom))))

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
    (ss/button :text "Save" :id :save-button)
    (ss/button :text "Aliases" :id :aliases-button)]))


(defn grid-frame [state-atom]
  (let [cell-setter     (partial controller/set-cell-at! state-atom)
        cell-getter     (partial controller/get-value-at state-atom)
        editable-getter (partial controller/get-editable-value-at state-atom)
        egg-loader      (fn [file grid]
                          (controller/load-egg file {:graph-atom (:graph @state-atom)
                                                     :grid       grid}))
        model           (table-model cell-getter cell-setter)
        grid            (grid model editable-getter)
        frame           (ss/frame :title "eggshell"
                                  :content (ss/border-panel
                                            :north (toolbar)
                                            :center
                                            (ss/border-panel
                                             :north  (code-editor/code-editor)
                                             :center (ss/scrollable grid))
                                            :south (status-area))
                                  :on-close :dispose)]
    (wire! {:frame           frame
            :state-atom      state-atom
            :table-model     model
            :cell-setter     cell-setter
            :editable-getter editable-getter
            :egg-loader      egg-loader})
    (ss/invoke-later (doto frame
                       ss/pack!
                       ;;(.setLocationRelativeTo nil)
                       ss/show!))))


(defn frames []
  (java.awt.Frame/getFrames))

;;(grid-frame state/graph-atom)
;;(eggshell.gui/grid-frame eggshell.state/egg-atom)
;;(eggshell.controller/load-egg "test-resources/first.egg")
;;(def tt (ss/select (last (frames)) [:#grid]))
