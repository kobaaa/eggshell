(ns eggshell.gui.menu
  (:require [seesaw.core :as ss]
            [eggshell.util :refer [cfuture]]))


(defn grid-menu [{:keys [value split-result-fn] :as opts}]
  (when value
    (let [{:keys [original-value]} value]
      (if (and original-value (seqable? original-value))
        [(ss/menu-item :text "Split result down (dynamic)"
                       :listen [:action (fn [_] (cfuture (split-result-fn (assoc opts :dynamic true))))])
         (ss/menu-item :text "Split result down (static)"
                       :listen [:action (fn [_] (cfuture (split-result-fn (assoc opts :dynamic false))))])]))))
