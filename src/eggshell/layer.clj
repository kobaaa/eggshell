(ns eggshell.layer
  (:require [eggshell.graph :as graph]
            [eggshell.gui.defaults :as gui.defaults]
            [seesaw.core :as ss])
  (:import [java.awt.image BufferedImage]))


(defn pass [{:keys [column-count row-count column-name value set-value! renderer] :as layer-below}]
  {:type         "pass"
   :column-count (fn [] (column-count))
   :row-count    (fn [] (row-count))
   :column-name  (fn [col] (column-name col))
   :value        (fn [row-col] (value row-col))
   :set-value!   (fn [row-col value] (set-value! row-col value))
   :renderer     (fn [row-col value] (renderer row-col value))
   :config!      (fn [_])})


(defn grid [cell-getter cell-setter]
  (let [label (ss/label)]
    {:type         "grid"
     :column-count (constantly 50)
     :row-count    (constantly 1048576)
     :column-name  (partial graph/idx->column)
     :value        cell-getter
     :set-value!   cell-setter
     :renderer     (fn [row-col {:keys [render-value]}]
                     (ss/config! label :text render-value))}))


(defn errors [{:keys [value renderer] :as layer-below}]
  (let [label (ss/label)]
    (merge (pass layer-below)
           {:type     "errors"
            :renderer (fn [row-col {:keys [render-value :rakk/error? :rakk/error-type] :as v}]
                        (let [c (renderer row-col v)]
                          (if-not error?
                            c
                            (ss/config! label
                                        :text render-value
                                        :halign :center
                                        :foreground (if (= error-type :primary) :white :black)
                                        :background (if (= error-type :primary)
                                                      gui.defaults/primary-error-color
                                                      gui.defaults/secondary-error-color)))))})))


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
                                   ::selected? ::has-focus?] :as v}]
                        (if (instance? java.awt.Image original-value)
                          (do (reset! image original-value)
                              panel)
                          (renderer row-col v)))})))


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



;;org.scilab.forge/jlatexmath {:mvn/version "1.0.7"}
;;\cos (2\theta) = \cos^2 \theta - \sin^2 \theta
(comment
 (do
   (import '[org.scilab.forge.jlatexmath TeXFormula])
   (let [f    (TeXFormula. a0)
         icon (.build (org.scilab.forge.jlatexmath.TeXFormula$TeXIconBuilder. f))]
     icon)))



;; com.hypirion/clj-xchart {:mvn/version "0.2.0"}
;; chart com.hypirion.clj-xchart

(comment
  (let [r (java.util.Random. 42)]
    (chart/as-buffered-image
     (chart/xy-chart
      {"Maxime" {:x (range 10)
                 :y (mapv #(+ % (* 3 (.nextDouble r)))
                          (range 10))}
       "Tyrone" {:x (range 10)
                 :y b1}}
      {:title  b2
       :width  400
       :height 300
       :x-axis {:title b3}
       :y-axis {:title b4
                :decimal-pattern "##.## km"}
       :theme  :ggplot2}))))

;;then, in b1: [50 35 35 40 30 40 70 80 75 70]
;;b2: Longest running distance
;;b3: Months (since start)
;;b4: Distance


;;;...use something like this for sparklines:

(comment
 (chart/as-buffered-image
  (chart/xy-chart
   {"Maxime" {:x (range 10)
              :y b1
              :style {:marker-type :none}}}
   {:width 400
    :height 50
    :chart {:padding 0}
    :plot {:background-color :white :content-size 1}
    :legend {:visible? false}
    :axis {:ticks {:visible? false}}
    :theme :ggplot2})))


;; {net.mikera/imagez {:mvn/version "0.12.0"}}
;; i     mikera.image.core
;; filt  mikera.image.filters
;; (img/load "/Users/sideris/Downloads/toys.jpg")
;; or
;; (img/load "/Users/sideris/Downloads/santa.jpg")

;;b0: (i/filter-image a0 (filt/invert))
;;c0: (i/filter-image b0 (filt/blur))
;;a1: (i/filter-image a0 (filt/grayscale))
;;b1: (i/filter-image a1 (filt/emboss))
;;a2: (i/filter-image a1 (filt/edge))
