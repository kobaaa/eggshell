(ns eggshell.io.v1
  (:require [eggshell :as e]
            [clojure.edn :as edn]
            [loom.graph :as loom]
            [loom.attr :as attr]
            [eggshell.io :as io]))


(defn strip-extras [g]
  (reduce (fn [g node]
            (-> g
                (attr/remove-attr node ::code)
                (attr/remove-attr node :function)
                (attr/remove-attr node :value)
                (attr/remove-attr node :error)))
          g (loom/nodes g)))


(defn save-egg [g filename]
  (spit filename
        (merge
         {::e/file-type "eggshell spreadsheet"
          ::e/version   "v1"}
         (update g ::e/graph #(into {} (strip-extras %))))))


(defn load-egg [filename]
  (-> filename
      slurp
      edn/read-string
      (update ::e/graph loom/map->BasicEditableDigraph)
      (io/assert-version "v1")))
