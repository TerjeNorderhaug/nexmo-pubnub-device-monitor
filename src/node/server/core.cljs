(ns server.core
  (:require-macros
   [cljs.core.async.macros :as m :refer [go go-loop alt!]])
  (:require
   [polyfill.compat]
   [cljs.nodejs :as nodejs]
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [reagent.core :as reagent :refer [atom]]
   [app.core :refer [static-page active-devices monitor-devices track-devices guard-devices]]
   [server.nexmo :as nexmo]))

(enable-console-print!)

(def express (nodejs/require "express"))

(defn handler [req res]
  (go
    (.set res "Content-Type" "text/html")
    (.send res (<! (static-page)))))

(def instructions-url "https://github.com/TerjeNorderhaug/nexmo-pubnub-device-monitor/blob/master/README.md")

(defn server [port success]
  (doto (express)
    (.get "/" handler)
    (.get "/devices"
          (fn [req res]
            (go (.json res (clj->js (map second (<! (active-devices))))))))
    (.get "/testing"
          (fn [req res]
            (.redirect res instructions-url)))
    (.use (.static express "resources/public"))
    (.listen port success)))

(defn -main [& mess]
  (nexmo/init-nexmo)
  (monitor-devices)
  (track-devices)
  (guard-devices nexmo/send-notification)
  (let [port (or (.-PORT (.-env js/process)) 1337)]
    (server port
            #(println (str "Server running at http://127.0.0.1:" port "/")))))

(set! *main-cli-fn* -main)
