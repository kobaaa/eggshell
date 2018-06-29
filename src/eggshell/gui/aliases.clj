(ns eggshell.gui.aliases
  (:require [seesaw.core :as ss]
            [seesaw.border :as border]
            [seesaw.keymap :as keymap]
            [eggshell.gui.defaults :as defaults]
            [eggshell.gui.common :as common]
            [eggshell.util :refer [cfuture]]))


(defn- aliases-panel []
  (ss/border-panel
   :north  (ss/label :text   " Enter alias-namespace pairs on each line:"
                     :font   defaults/small-font
                     :border (common/panel-border {:top 8 :left 6 :right 6 :bottom 5}))
   :center (ss/scrollable (ss/text :id          :text-area
                                   :multi-line? true
                                   :font        defaults/mono-font)
                          :border (border/to-border defaults/textbox-border
                                                    (common/panel-border {:thickness 6})))
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


(defn aliases-frame [aliases {:keys [parent apply-fn]}]
  (doto (ss/frame :title "Edit aliases"
                  :content (aliases-panel)
                  :on-close :dispose
                  :size [400 :by 200])
    (wire! apply-fn)
    (ss/value! {:text-area aliases})
    (.setLocationRelativeTo parent)
    ss/show!))


;;(-> (eggshell.gui.aliases/aliases-frame "" {:apply-fn prn :parent (first (frames))}) ss/show!)
