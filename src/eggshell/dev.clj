(ns eggshell.dev
  (:import [java.awt Toolkit AWTEvent]
           [java.awt.event AWTEventListener]))

;; see https://tips4java.wordpress.com/2009/08/30/global-event-listeners/
;; not super-useful because of this bug: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6292132

(def all-events-mask ;;11111110111111111111
  (bit-or
   AWTEvent/ACTION_EVENT_MASK
   AWTEvent/ADJUSTMENT_EVENT_MASK
   ;;AWTEvent/COMPONENT_EVENT_MASK
   ;;AWTEvent/CONTAINER_EVENT_MASK
   AWTEvent/FOCUS_EVENT_MASK
   ;;AWTEvent/HIERARCHY_BOUNDS_EVENT_MASK
   ;;AWTEvent/HIERARCHY_EVENT_MASK
   AWTEvent/INPUT_METHOD_EVENT_MASK
   AWTEvent/INVOCATION_EVENT_MASK
   AWTEvent/ITEM_EVENT_MASK
   AWTEvent/KEY_EVENT_MASK
   ;;AWTEvent/MOUSE_EVENT_MASK
   ;;AWTEvent/MOUSE_MOTION_EVENT_MASK
   ;;AWTEvent/MOUSE_WHEEL_EVENT_MASK
   ;;AWTEvent/PAINT_EVENT_MASK
   AWTEvent/TEXT_EVENT_MASK
   AWTEvent/WINDOW_EVENT_MASK
   AWTEvent/WINDOW_FOCUS_EVENT_MASK
   AWTEvent/WINDOW_STATE_EVENT_MASK))

(def all-events-mask ;;11111110111111111111
  (bit-or
   AWTEvent/ACTION_EVENT_MASK
   ;;AWTEvent/ADJUSTMENT_EVENT_MASK
   ;;AWTEvent/COMPONENT_EVENT_MASK
   ;;AWTEvent/CONTAINER_EVENT_MASK
   ;;AWTEvent/FOCUS_EVENT_MASK
   ;;AWTEvent/HIERARCHY_BOUNDS_EVENT_MASK
   ;;AWTEvent/HIERARCHY_EVENT_MASK
   ;;AWTEvent/INPUT_METHOD_EVENT_MASK
   ;;AWTEvent/INVOCATION_EVENT_MASK
   ;;AWTEvent/ITEM_EVENT_MASK
   AWTEvent/KEY_EVENT_MASK
   ;;AWTEvent/MOUSE_EVENT_MASK
   ;;AWTEvent/MOUSE_MOTION_EVENT_MASK
   ;;AWTEvent/MOUSE_WHEEL_EVENT_MASK
   ;;AWTEvent/PAINT_EVENT_MASK
   ;;AWTEvent/TEXT_EVENT_MASK
   ;;AWTEvent/WINDOW_EVENT_MASK
   ;;AWTEvent/WINDOW_FOCUS_EVENT_MASK
   ;;AWTEvent/WINDOW_STATE_EVENT_MASK
   ))




(defn log-swing-events! []
  (-> (Toolkit/getDefaultToolkit)
      (.addAWTEventListener
       (reify AWTEventListener
         (eventDispatched [this event]
           (prn 'EVENT event)))
       all-events-mask)))

(defn stop-logging-swing-events! []
  (let [tk (Toolkit/getDefaultToolkit)]
    (for [listener (.getAWTEventListeners tk)]
      (.removeAWTEventListener tk listener))))
