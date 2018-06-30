(ns eggshell.util)

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
