(ns eggshell.io.v1
  (:require [clojure.edn :as edn]
            [loom.graph :as loom]
            [loom.attr :as attr]
            [eggshell.io :as io]))


(defn strip-extras [g]
  (reduce (fn [g node]
            (-> g
                (attr/remove-attr node ::code)
                (attr/remove-attr node :function)))
          g (loom/nodes g)))


(defn save-eggshell [g filename]
  (spit filename
        {:eggshell/file-type "eggshell spreadsheet"
         :eggshell/version   "v1"
         :eggshell/graph     (into {} (strip-extras g))}))


(defn load-eggshell [filename]
  (-> filename
      slurp
      edn/read-string
      (update :eggshell/graph loom/map->BasicEditableDigraph)
      (io/assert-version "v1")))
