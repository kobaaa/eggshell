(ns eggshell.gui.menu
  (:require [seesaw.core :as ss]
            [eggshell.util :refer [cfuture]]))


(defn- split-result-items [split-result-fn opts]
  [(ss/menu-item :text "Split result down (dynamic)"
                 :listen [:action (fn [_] (cfuture (split-result-fn (assoc opts
                                                                           :dynamic true
                                                                           :direction :down))))])
   (ss/menu-item :text "Split result right (dynamic)"
                 :listen [:action (fn [_] (cfuture (split-result-fn (assoc opts
                                                                           :dynamic true
                                                                           :direction :right))))])
   (ss/menu-item :text "Split result down (static)"
                 :listen [:action (fn [_] (cfuture (split-result-fn (assoc opts
                                                                           :dynamic false
                                                                           :direction :down))))])
   (ss/menu-item :text "Split result right (static)"
                 :listen [:action (fn [_] (cfuture (split-result-fn (assoc opts
                                                                           :dynamic false
                                                                           :direction :right))))])])


(defn- arrow-type [{:keys [raw-code]}]
  (try
    (let [code (read-string raw-code)]
      (condp = (first code)
        '-> "->"
        '->> "->>"
        ;;'as-> "as->" ;;TODO
        ;;'as->> "as->>" ;;TODO
        :else nil))
    (catch Exception _ nil)))


(defn grid-menu [{:keys [value split-result-fn expand-arrow-fn] :as opts}]
  (when value
    (let [{:keys [original-value]} value]
      (remove
       nil?
       (concat

        (when (and original-value (seqable? original-value))
          (split-result-items split-result-fn opts))

        (when-let [arrow (arrow-type value)]
          [(ss/menu-item :text (str "Expand " arrow " down")
                         :listen [:action (fn [_] (cfuture (expand-arrow-fn (assoc opts :direction :down))))])
           (ss/menu-item :text (str "Expand " arrow " right")
                         :listen [:action (fn [_] (cfuture (expand-arrow-fn (assoc opts :direction :right))))])]))))))
