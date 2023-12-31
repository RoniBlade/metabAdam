(ns metabuild-common.entrypoint
  (:require
   [metabuild-common.output :as out]))

(set! *warn-on-reflection* true)

(defn do-exit-when-finished-nonzero-on-exception
  "Impl for [[exit-when-finished-nonzero-on-exception]]."
  [thunk]
  (try
    (thunk)
    (System/exit 0)
    (catch Throwable e
      (out/pretty-print-exception e)
      (System/exit -1))))

(defmacro exit-when-finished-nonzero-on-exception
  "Execute `body` and catch exceptions. If an Exception is thrown, exit with status code 0; if an exception was
  thrown, print it and exit with a non-zero status code. Intended for use in `-main` functions."
  {:style/indent 0}
  [& body]
  `(do-exit-when-finished-nonzero-on-exception (fn [] ~@body)))
