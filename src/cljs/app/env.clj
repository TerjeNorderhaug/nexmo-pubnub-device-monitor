(ns app.env
  (:require
   [environ.core :as env]))

(defmacro env
  "Resolve environment vars at compile time for security"
  ([name]
   (env/env name)))

(defmacro environment
  "Warning: May result in exposing secret environment vars"
  ([]
   env/env))
