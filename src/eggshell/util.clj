(ns eggshell.util
  (:require [clojure.string :as str]))

(defmacro with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))


(defmacro cfuture [& body]
  `(future
     (try
       (let [cl# (.getContextClassLoader (Thread/currentThread))]
         (.setContextClassLoader (Thread/currentThread) (clojure.lang.DynamicClassLoader. cl#)))
       (do ~@body)
       (catch Exception e#
         (.printStackTrace e#)
         (throw e#)))))


(defmacro unspam [monitor & body]
  `(when-not (= ::already-running (deref ~monitor))
     (try
       (reset! ~monitor ::already-running)
       (do ~@body)
       (finally
         (reset! ~monitor nil)))))


(defn mac? []
  (-> "os.name"
      (System/getProperty)
      str/lower-case
      (str/includes? "mac")))
