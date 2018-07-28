(ns eggshell.main
  (:require [eggshell.gui]
            [eggshell.state]
            [eggshell.api.img]
            [eggshell.api.math])
  (:gen-class))

(defn -main
  []
  (eggshell.gui/grid-frame eggshell.state/egg-atom))
