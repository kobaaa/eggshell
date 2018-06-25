(ns eggshell.gui.defaults
  (:require [seesaw.font :as font]))

(def mono-font (font/font :name "Monaco" :size 12))
(def text-font (font/default-font "TextField.font"))

(def selected-color "#b4d8fd")
(def primary-error-color "#ff5555")
(def secondary-error-color "#ff9191")
