(ns app.start
  (:require [app.core :as app]))

(enable-console-print!)

(defn ^:export main []
  (app/activate))

(set! js/main-cljs-fn main)
