(ns eggshell.io.clipboard)

(def ^:private app-buffer (atom nil))

(defn- get-clipboard []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn get-from-buffer []
  (try
    (.getTransferData (.getContents (get-clipboard) nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
    (catch java.lang.NullPointerException e nil)))

(defn copy-to-buffer [text]
  (.setContents (get-clipboard) (java.awt.datatransfer.StringSelection. text) nil))

(defn code-from-app-buffer []
  (:raw-code @app-buffer))

(defn value-from-app-buffer []
  (:original-value @app-buffer))

(defn set-app-buffer [value]
  (reset! app-buffer value))
