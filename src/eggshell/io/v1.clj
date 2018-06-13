(ns eggshell.io.v1
  (:require [clojure.edn :as edn]
            [loom.graph :as loom]
            [eggshell.graph :as graph]
            [eggshell.io :as io]))

(defn save-eggshell [g filename]
  (spit filename
        {:eggshell/file-type "eggshell spreadsheet"
         :eggshell/version   "v1"
         :eggshell/graph     (into {} (graph/strip-extras g))}))

(defn load-eggshell [filename]
  (-> filename
      slurp
      edn/read-string
      (update :eggshell/graph loom/map->BasicEditableDigraph)
      (io/assert-version "v1")))
