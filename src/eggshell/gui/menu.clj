(ns eggshell.gui.menu
  (:require [seesaw.core :as ss]
            [eggshell.io.clipboard :as cb]
            [eggshell.util :refer [cfuture]]))

(defn- split-result-items [split-result-fn opts]
  [(ss/separator)
   (ss/menu-item :text "Split result down (dynamic)"
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

(defn grid-menu [{:keys [value split-result-fn expand-arrow-fn set-cell-fn] :as opts}]
  (when value
    (let [{:keys [original-value raw-code]} value]
      (remove
       nil?
       (concat
        [(ss/menu-item :text "Copy"
                       :listen [:action (fn [_]
                                          (cb/copy-to-buffer  (str original-value))
                                          (reset! cb/app-buffer value))])
         (ss/menu-item :text "Paste Value"
                       :listen [:action (fn [_] (set-cell-fn (:original-value @cb/app-buffer)))])
         (ss/menu-item :text "Paste Formula"
                       :listen [:action (fn [_] (set-cell-fn (:raw-code @cb/app-buffer)))])
         (ss/separator)
         (ss/menu-item :text "Paste from Clipboard"
                       :listen [:action (fn [_] (set-cell-fn (cb/get-from-buffer)))])]

        (when (and original-value (seqable? original-value))
          (split-result-items split-result-fn opts))

        (when-let [arrow (arrow-type value)]
          [(ss/separator)
           (ss/menu-item :text (str "Expand " arrow " down")
                         :listen [:action (fn [_] (cfuture (expand-arrow-fn (assoc opts :direction :down))))])
           (ss/menu-item :text (str "Expand " arrow " right")
                         :listen [:action (fn [_] (cfuture (expand-arrow-fn (assoc opts :direction

                                                                                   :right))))])]))))))


