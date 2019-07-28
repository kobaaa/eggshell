(ns eggshell.io.clipboard)

(defn- get-clipboard []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn get-from-buffer []
  (try
    (.getTransferData (.getContents (get-clipboard) nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
    (catch java.lang.NullPointerException e nil)))

(defn copy-to-buffer [text]
  (.setContents (get-clipboard) (java.awt.datatransfer.StringSelection. text) nil))

(def app-buffer (atom nil))

