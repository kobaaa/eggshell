(ns eggshell.layer
  (:require [eggshell.graph :as graph]
            [seesaw.core :as ss])
  (:import [java.awt.image BufferedImage]))

(defn grid [cell-getter cell-setter]
  (let [label (ss/label)]
    {:type         "grid"
     :column-count (constantly 50)
     :row-count    (constantly 1048576)
     :column-name  (partial graph/idx->column)
     :value        cell-getter
     :set-value!   cell-setter
     :renderer     (fn [row-col {:keys [render-value]}]
                     (doto label (.setText render-value)))}))


(defn pass [{:keys [column-count row-count column-name value set-value! renderer] :as layer-below}]
  {:type         "pass"
   :column-count (fn [] (column-count))
   :row-count    (fn [] (row-count))
   :column-name  (fn [col] (column-name col))
   :value        (fn [row-col] (value row-col))
   :set-value!   (fn [row-col value] (set-value! row-col value))
   :renderer     (fn [row-col value] (renderer row-col value))
   :config!      (fn [_])})


(defn image-render [{:keys [value renderer] :as layer-below}]
  (let [image  (atom nil)
        panel  (proxy [javax.swing.JPanel] []
                 (paintComponent [g]
                   (when-let [i @image]
                     (.drawImage g i 0 0 this))))
        config {:selection   nil
                :fit-to-cell true}]
    (merge (pass layer-below)
           {:type     "image-render"
            :renderer (fn [row-col
                           {:keys [original-value render-value cell-id :rakk/error? :rakk/error-type
                                   ::selected? ::has-focus?]}]
                        (if (instance? java.awt.Image original-value)
                          (do (reset! image original-value)
                              panel)
                          (renderer row-col value)))})))


(defn to-model [{:keys [column-count row-count column-name value set-value!] :as layer}]
  (proxy [javax.swing.table.DefaultTableModel] [] ;;TODO look into using reify
    (getColumnCount [] (column-count))
    (getRowCount [] (row-count))
    (isCellEditable [row col] true)
    (getColumnName [col] (column-name col))
    (getValueAt [row col] (value [row col]))
    (setValueAt [value row col] (set-value! [row col] value))
    (getColumnClass [c] Object)))


(defn to-cell-renderer [{:keys [renderer] :as layer}]
  (proxy [javax.swing.table.DefaultTableCellRenderer] [] ;;TODO look into using reify
    (getTableCellRendererComponent [table value is-selected has-focus row col]
      (renderer [row col] (merge value
                                 {::selected?  is-selected
                                  ::has-focus? has-focus})))))


;;(require '[clojure.java.io :as io])
;;(javax.imageio.ImageIO/read (io/file "/Users/sideris/Downloads/toys-thumb.jpg"))
;;(let [x a0] (javax.imageio.ImageIO/read (io/file "/Users/sideris/Downloads/toys-thumb.jpg")))
