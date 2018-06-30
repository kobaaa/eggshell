(ns eggshell.gui.code-editor
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [eggshell.gui.defaults :as defaults]
            [eggshell.gui.common :as common]
            [cross-parinfer.core :as par]
            [clojure.string :as str]))

;; https://github.com/shaunlebron/parinfer/blob/master/lib/doc/integrating.md

(defn- offset->row-col [text pos]
  (let [idx (vec (cons 0 (reductions + (map #(inc (.length %)) (str/split text #"\n")))))
        row (dec (count (take-while #(<= % pos) idx)))
        col (- pos (get idx row))]
    [row col]))


(defn- row-col->offset [text [row col]]
  (let [idx (vec (cons 0 (reductions + (map #(inc (.length %)) (str/split text #"\n")))))]
    (+ col (get idx row))))


(defn- parinfer! [editor]
  (ss/invoke-later ;;to allow updates to catch up
   (let [old-text         (.getText editor)
         pos              (.getCaretPosition editor)
         [row col]        (offset->row-col old-text pos)
         {:keys [text x]} (par/indent-mode old-text col row)]
     (when-not (= old-text text)
       (doto editor
         (.setText text)
         (.setCaretPosition (row-col->offset text [row x])))))))


(defn code-editor []
  (let [editor (ss/text :id          :code-editor
                        :font        defaults/mono-font
                        ;;:editable?   false
                        :multi-line? true
                        :border      (border/to-border defaults/textbox-border
                                                       (common/panel-border {:thickness 6})))]
    (ss/listen (.getDocument editor) :document (fn [_] (parinfer! editor)))
    ;; (ss/listen editor :caret (fn [_]
    ;;                            (let [p (.getCaretPosition editor)]
    ;;                              (prn p (offset->row-col (.getText editor) p)))))
    editor))


(defn add-indent! [editor text type]
  (let [pos                            (.getCaretPosition editor)
        {:keys [cursor-position text]} (par/add-indent {:text            text
                                                        :cursor-position [pos pos]
                                                        :indent-type     type})
        [p1 p2]                        cursor-position]
    (doto editor
      (.setText text)
      (.setCaretPosition p1)
      (.moveCaretPosition p2))))


(defn insert-new-line! [editor]
  (let [pos  (.getCaretPosition editor)
        text (.getText editor)]
    (doto editor
      (.setText (str (subs text 0 pos) "\n  " (subs text pos)))
      (.setCaretPosition (+ 3 (.length (subs text 0 pos)))))
    (parinfer! editor)))
