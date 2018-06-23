(ns eggshell.gui.code-editor
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [eggshell.gui.defaults :as defaults]))

(defn code-editor []
  (let [border (.getBorder (ss/text))]
    (ss/config!
     (ss/text :id :code-editor
              :font defaults/mono-font
              :multi-line? true)
     :border (border/to-border border (border/line-border :color "#eeeeee" :thickness 6)))))
