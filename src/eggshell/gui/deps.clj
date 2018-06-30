(ns eggshell.gui.deps
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [seesaw.keymap :as keymap]
            [eggshell.gui.defaults :as defaults]
            [eggshell.gui.common :as common]
            [eggshell.gui.code-editor :as code-editor]
            [eggshell.util :refer [cfuture]]))


(defn- aliases-panel []
  (ss/border-panel
   :north  (ss/label :text   " Enter deps"
                     :font   defaults/small-font
                     :border (common/panel-border {:top 8 :left 6 :right 6 :bottom 5}))
   :center (ss/scrollable (code-editor/code-editor))
   :south  (ss/horizontal-panel
            :items [(ss/button :id :cancel-button :text "Cancel")
                    (ss/button :id :apply-button :text "Apply")
                    (ss/button :id :ok-button :text "OK")])))


(defn wire! [frame apply-fn]
  (let [{:keys [text-area cancel-button apply-button ok-button]} (ss/group-by-id frame)]
    (keymap/map-key frame "ESCAPE" (fn [_] (ss/dispose! frame)))

    (ss/listen cancel-button :action (fn [_] (ss/dispose! frame)))
    (ss/listen apply-button  :action (fn [_]
                                       (cfuture
                                         (apply-fn (ss/value text-area)))))
    (ss/listen ok-button     :action (fn [_]
                                       (cfuture
                                         (apply-fn (ss/value text-area))
                                         (ss/invoke-later (ss/dispose! frame)))))))


(defn deps-frame [aliases {:keys [parent apply-fn]}]
  (doto (ss/frame :title "Edit deps"
                  :content (aliases-panel)
                  :on-close :dispose
                  :size [400 :by 200])
    (wire! apply-fn)
    (ss/value! {:code-editor aliases})
    (.setLocationRelativeTo parent)
    ss/show!))
