(ns eggshell.gui
  (:require [seesaw.core :as ss]
            [seesaw.color :as color]
            [seesaw.keymap :as keymap]
            [seesaw.chooser :as chooser]
            [seesaw.border :as border]
            [seesaw.color :as color]
            [seesaw.border :as border]
            [seesaw.dev :as dev]
            [rakk.core :as rakk]
            [eggshell :as e]
            [eggshell.analyze :as analyze]
            [eggshell.controller :as controller]
            [eggshell.gui.table :as table]
            [eggshell.gui.code-editor :as code-editor]
            [eggshell.gui.defaults :as defaults]
            [eggshell.gui.common :as common]
            [eggshell.gui.aliases :as aliases]
            [eggshell.gui.deps :as deps]
            [eggshell.gui.grid :as grid]
            [eggshell.state :as state]
            [eggshell.layer :as layer]
            [eggshell.util :as util :refer [cfuture]]
            [clojure.repl :as repl]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [eggshell.graph :as graph]))


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
            :id :value-area
            :visible? false
            :border 3
            :preferred-size [100 :by 200]
            :items
            [(ss/scrollable
              (ss/text :id :cell-value-area :multi-line? true :font defaults/mono-font))])))


(defn- cell-value-text [value]
  (str
   "type: " (or (some-> value type .getName) "nil")
   "\n\n"
   (with-out-str (pp/pprint value))))


(defn- error-trace-text [error]
  (str
   (when (some-> error ex-data :rakk/secondary-error some?)
     (str "Some upstream cells contain errors: "
          (str/join ", " (->> error ex-data :rakk/upstream-errors (map (comp name :node)) sort))
          "\n\n"))
   (str/replace (util/with-err-str (repl/pst error))
                "\t" "   ")))


(defn update-status-area! [status-area cell-value-area grid graph]
  (let [[row col] (table/selected-cell grid)]
    (if (and row col)
      (let [cell-id                                    (graph/coords->id row col)
            {:rakk/keys [error? error] :as error-info} (rakk/error-info graph cell-id)]
        (ss/value! status-area
                   {:status-line     (status-line-text {:graph   graph
                                                        :cell-id cell-id
                                                        :error?  error})
                    :cell-value-area (if-not error?
                                       (cell-value-text (graph/value graph cell-id))
                                       (error-trace-text error))})
        (ss/scroll! cell-value-area :to :top))
      (ss/value! status-area
                 {:status-line     "No selection"
                  :cell-value-area "No errors"}))))


(defn- update-code-editor! [code-editor cell-id-label grid editable-getter]
  (let [selected (table/selected-cell grid)]
         (if-not selected
           (do
             (ss/value! cell-id-label "N/A")
             (ss/config! code-editor
                         :text      ""
                         :editable? false))
           (do
             (ss/value! cell-id-label (str (name (graph/coords->id (first selected) (second selected)))
                                           " ="))
             (ss/config! code-editor
                         :text      (editable-getter selected)
                         :editable? true)))))


(defn wire! [{:keys [frame state-atom table-model cell-setter editable-getter egg-loader grid-table grid-scroll-pane]}]
  (let [{:keys [load-button save-button deps-button aliases-button
                cell-id-label code-editor grid
                status-area status-line value-area cell-value-area]}
        (ss/group-by-id frame)
        graph (::e/graph @state-atom)]

    ;;focus on grid when window is ready
    (ss/listen frame :window-opened
               (fn [_]
                 (table/set-selection! grid [0 0])
                 (ss/request-focus! grid)))

    ;;update table when grid graph changes
    (add-watch state-atom :kk
               (fn [_ _ _ _]
                 (table/save-selection
                  grid
                  #(ss/invoke-now (do
                                    (.fireTableDataChanged table-model)
                                    (grid/apply-row-heights grid-table grid-scroll-pane))))))

    ;;listen for cell selection changes to update code editor
    (table/listen-selection
     grid
     (fn [_]
       (update-code-editor! code-editor cell-id-label grid editable-getter)
       (.repaint frame))) ;;TODO also on column/row resize, and on scroll

    (keymap/map-key grid "meta META" common/nothing)

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
                      (let [row-col (table/selected-cell grid)
                            pos     (ss/config code-editor :caret-position)]
                        (cfuture
                         (cell-setter row-col (ss/value code-editor))
                         (ss/invoke-now
                          (table/set-selection! grid row-col)
                          (ss/config! code-editor :caret-position pos)))))
                    :scope :self)


    (keymap/map-key code-editor "control ENTER"
                    (fn [_] (code-editor/insert-new-line! code-editor)))


    ;;wire up toolbar buttons

    (ss/listen load-button :action
               (fn [_]
                 (when-let [file (chooser/choose-file)]
                   ;;TODO move to future?
                   (egg-loader file grid))))

    (ss/listen save-button :action
               (fn [_]
                 (when-let [file (chooser/choose-file :type :save)]
                   ;;TODO move to future?
                   (controller/save-egg file {::e/graph         graph
                                              ::e/column-widths (table/column-widths grid)}))))

    (ss/listen deps-button :action
               (fn [_]
                 (common/show-frame-once
                  (deps/deps-frame (::e/deps @state-atom)
                                   {:parent frame
                                    :apply-fn (partial controller/set-deps! state-atom)}))))

    (ss/listen aliases-button :action
               (fn [_]
                 (common/show-frame-once
                  (aliases/aliases-frame (::e/aliases @state-atom)
                                         {:parent frame
                                          :apply-fn (partial controller/set-aliases! state-atom)}))))

    ;;wire up status area
    (table/listen-selection grid (fn [_] (update-status-area! status-area cell-value-area grid (::e/graph @state-atom))))

    (letfn [(toggle-value-area [_] (ss/config! value-area
                                               :visible? (not (ss/config value-area :visible?))
                                               :preferred-size [(.getWidth value-area) :by 200]))]

      (ss/listen status-line :mouse-clicked toggle-value-area)
      (keymap/map-key frame "meta E" toggle-value-area)
      (keymap/map-key grid "meta E" toggle-value-area))))


(defn- toolbar []
  (ss/flow-panel
   :align :left
   :items
   [(ss/button :text "Load" :id :load-button)
    (ss/button :text "Save" :id :save-button)
    (ss/button :text "Deps" :id :deps-button)
    (ss/button :text "Aliases" :id :aliases-button)]))


(defn grid-frame [state-atom]
  (let [cell-setter     (partial controller/set-cell-at! state-atom)
        cell-getter     (partial controller/get-value-at state-atom)
        editable-getter (partial controller/get-editable-value-at state-atom)
        egg-loader      (fn [file grid]
                          (controller/load-egg file {:graph-atom (::e/graph @state-atom)
                                                     :grid       grid}))
        layer           (-> (layer/grid cell-getter cell-setter)
                            (layer/image-render)
                            (layer/errors))
        grid            (grid/make-grid layer editable-getter)
        frame           (ss/frame :title "eggshell"
                                  :content (ss/border-panel
                                            :north (toolbar)
                                            :center
                                            (ss/border-panel
                                             :north  (ss/border-panel
                                                      :west   (ss/label :id :cell-id-label
                                                                        :font defaults/mono-font
                                                                        :text "N/A ="
                                                                        :border (border/empty-border :left 14))
                                                      :center (code-editor/code-editor))
                                             :center (:scroll-pane grid))
                                            :south (status-area))
                                  :on-close :dispose)]

    (-> frame .getRootPane (.setGlassPane (grid/glass-pane (.getRootPane frame) (:table grid))))
    (-> frame .getRootPane .getGlassPane (.setVisible true))

    (wire! {:frame            frame
            :state-atom       state-atom
            :table-model      (.getModel (:table grid))
            :grid-table       (:table grid)
            :grid-scroll-pane (:scroll-pane grid)
            :cell-setter      cell-setter
            :editable-getter  editable-getter
            :egg-loader       egg-loader})
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

(comment
 {org.clojure/core.memoize {:mvn/version "0.7.1"}
  clj-time                 {:mvn/version "0.14.4"}})
