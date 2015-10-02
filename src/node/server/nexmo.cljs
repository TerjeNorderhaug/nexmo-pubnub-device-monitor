(ns server.nexmo
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [app.env :refer [env]])
  (:require
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [cljs.nodejs :as nodejs]
   [app.debug :refer [echo]]))

(def nexmo (nodejs/require "easynexmo")) ; https://github.com/pvela/nexmo

(def nexmo-key (env :nexmo-key))
(def nexmo-secret (env :nexmo-secret))

(defn init-nexmo []
  (assert nexmo-key)
  (assert nexmo-secret)
  (.initialize nexmo nexmo-key nexmo-secret "https"))

(defn collect [ch err val]
  (when err
    (println "[NEXMO]" err))
  (when val
    (put! ch (js->clj val)))
  (close! ch))

(defn get-numbers [account]
  (let [result (chan)]
    (go
      (.getNumbers account (partial collect result)))
    result))

;; (echo (get-numbers nexmo))

(defn send-notification []
  (let [response (chan)]
    (.sendTTSMessage nexmo "16502913436" "Shuttle Offline" #js {} (partial collect response))
    response))

;; (echo (send-notification))
