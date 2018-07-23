(ns eggshell.api.img
  (:require [clojure.java.io :as io])
  (:refer-clojure :exclude [load]))

(defn load [file]
  (javax.imageio.ImageIO/read (io/file file)))
