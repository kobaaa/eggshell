(ns eggshell.gui.code-editor
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [eggshell.gui.defaults :as defaults]))

(defn code-editor []
  (let [border (.getBorder (ss/text))]
    (ss/config!
     (ss/text :id          :code-editor
              :font        defaults/mono-font
              :editable?   false
              :multi-line? true)
     :border (border/to-border border (border/line-border :color "#eeeeee" :thickness 6)))))


(defn insert-new-line! [editor]
  (let [pos  (.getCaretPosition editor)
        text (.getText editor)]
    (doto editor
      (.setText (str (subs text 0 pos)
                           "\n"
                           (subs text pos)))
      (.setCaretPosition (inc pos)))))
