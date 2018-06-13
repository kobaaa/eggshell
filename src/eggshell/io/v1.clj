(ns eggshell.io.v1
  (:require [clojure.edn :as edn]
            [loom.graph :as loom]
            [eggshell.graph :as graph]
            [eggshell.grid :as grid]
            [eggshell.io :as io]))

(defn save-eggshell [g filename]
  (spit filename
        {:eggshell/file-type "eggshell spreadsheet"
         :eggshell/version   "v1"
         :eggshell/graph     (grid/strip-extras g)}))

(defn load-eggshell [filename]
  (->> filename slurp
       (edn/read-string {:readers {'loom.graph.BasicEditableDigraph
                                   (fn [x]
                                     (loom/map->BasicEditableDigraph x))}})
       (io/assert-version "v1")))
