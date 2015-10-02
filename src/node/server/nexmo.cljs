(ns server.nexmo
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [app.env :refer [env]])
  (:require
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [cljs.nodejs :as nodejs]
   [app.debug :refer [echo]]))

(def nexmo (nodejs/require "easynexmo"))

(def nexmo-key (env :nexmo-key))
(def nexmo-secret (env :nexmo-secret))

(defn init-nexmo []
  (assert nexmo-key)
  (assert nexmo-secret)
  (.initialize nexmo nexmo-key nexmo-secret "https"))

(defn put1! [ch val]
  (when val
    (put! ch val))
  (close! ch))

(defn get-numbers [account]
  (let [result (chan)]
    (go
      (.getNumbers account #(put1! result %)))
    result))

;; (echo (get-numbers nexmo))

(defn send-notification []
  (let [response (chan)]
    (.sendTTSMessage nexmo "16502913436" "Shuttle Offline" #js {} #(put1! response %))
    response))

;; (echo (send-notification))
