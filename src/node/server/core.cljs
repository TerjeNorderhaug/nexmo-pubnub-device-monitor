(ns server.core
  (:require-macros
   [cljs.core.async.macros :as m :refer [go go-loop alt!]]
   [app.env :refer [env]])
  (:require
   [polyfill.compat]
   [cljs.nodejs :as nodejs]
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [reagent.core :as reagent :refer [atom]]
   [app.core :refer [static-page active-devices monitor-devices track-devices]]
   [app.pubnub :as pubnub]
   [server.nexmo :as nexmo]))

(enable-console-print!)

(def express (nodejs/require "express"))

(defn handler [req res]
  (if (= "https" (aget (.-headers req) "x-forwarded-proto"))
    (.redirect res (str "http://" (.get req "Host") (.-url req)))
    (go
      (.set res "Content-Type" "text/html")
      (.send res (<! (static-page))))))

(def instructions-url "https://github.com/TerjeNorderhaug/nexmo-pubnub-device-monitor/blob/master/README.md")

(defn server [port success]
  (doto (express)
    (.get "/" handler)
    (.get "/devices"
          (fn [req res]
            (go-loop [utime (pubnub/fetch-time)]
              (.json res (clj->js (map second (active-devices utime)))))))
    (.get "/testing"
          (fn [req res]
            (.redirect res instructions-url)))
    (.use (.static express "resources/public"))
    (.listen port success)))

(def devices-pubnub
  {:keepalive 5
   :restore true
   :ssl true
   :subscribe_key (env :pubnub-source-key)
   :channel (env :pubnub-source-channel)})

(defn -main [& mess]
  (nexmo/init-nexmo)
  (monitor-devices
   (pubnub/subscribe devices-pubnub))
  (track-devices nexmo/send-notification)
  (let [port (or (.-PORT (.-env js/process)) 1337)]
    (server port
            #(println (str "Server running at http://127.0.0.1:" port "/")))))

(set! *main-cli-fn* -main)
