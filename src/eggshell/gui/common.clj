(ns eggshell.gui.common
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [eggshell.gui.defaults :as defaults]))


(defn panel-border [opts]
  (apply border/line-border
         (apply concat
                (merge
                 {:color defaults/panel-color}
                 opts))))
