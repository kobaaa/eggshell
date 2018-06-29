(ns eggshell.gui.common
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [eggshell.gui.defaults :as defaults]))


(defn panel-border [opts]
  (apply border/line-border
         (apply concat
                (merge
                 {:color defaults/panel-color}
                 opts))))


(def show-frame-once-state (atom {}))

(defmacro show-frame-once
  "Prevent frame opening more than once. It is assumed that the body
  returns a JFrame."
  [& body]
  (let [k (hash &form)]
    `(if-let [existing# (get @show-frame-once-state ~k)]
       (.toFront existing#)
       (let [frame# (do ~@body)]
         (swap! show-frame-once-state assoc ~k frame#)
         (ss/listen frame# :window-closed
                    (fn [e#] (swap! show-frame-once-state dissoc ~k)))
         frame#))))
