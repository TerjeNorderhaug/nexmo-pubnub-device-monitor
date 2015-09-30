(ns app.debug
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [chan <! >! put! timeout]]))

(defn echo [ch]
  (go-loop []
    (when-let [val (<! ch)]
      (println ">>>>" val)
      (recur))))
