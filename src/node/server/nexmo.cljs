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

(defn get-numbers [account]
  (let [result (chan)]
    (go
      (.getNumbers account
                   #(do
                      (if %1
                        (put! result %1)
                        (println "[NEXMO] get numbers failed"))
                      (close! result))))
    result))

;; (echo (get-numbers nexmo))

(defn send-notification []
  (.sendTTSMessage nexmo "16502913436" "Shuttle Offline" nil nil))
