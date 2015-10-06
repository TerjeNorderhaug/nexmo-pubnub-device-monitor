(ns app.json
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [goog.net.XhrIo :as xhr]))

(defn fetch-json [uri]
  (let [out (chan)]
    (xhr/send uri (fn [e]
                    (put! out (-> e .-target .getResponseJson
                                  (js->clj :keywordize-keys true)))))
    out))
