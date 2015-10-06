(ns app.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [app.env :refer [env]])
  (:require
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [goog.dom :as dom]
   [goog.events :as events]
   [goog.history.EventType :as EventType]
   [reagent.core :as reagent :refer [atom]]
   [secretary.core :as secretary :refer-macros [defroute]]
   [app.views :refer [monitor-view monitor-page html5]]
   [app.pubnub :as pubnub])
  (:import goog.History))

(secretary/set-config! :prefix "#")

(def scripts [{:src "/js/out/app.js"}
              "main_cljs_fn()"])

(defonce devices-var (atom {}))

(defn guard-devices [alarm]
  (go-loop []
    (when alarm
      (println "ALARM!"))
    (<! (timeout (* 10 1000)))
    (recur)))

(defn track-devices []
  (go-loop []
    (when-let [val (<! (:in-chan (pubnub/tunnel)))]
      (reset! devices-var
              (update @devices-var (:id val) (fn [_] val)))
      (println ">>>>" val)
      (recur))))

(def devices-pubnub
  {:keepalive 5
   :restore true
   :ssl true
   :subscribe_key (env :pubnub-source-key)
   :channel (env :pubnub-source-channel)})

(defn monitor-devices []
  (let [in (pubnub/subscribe devices-pubnub)]
    (go-loop []
      (when-let [msg (<! in)]
        (println "[MONITOR]" msg)
        (pubnub/bidir-send (pubnub/tunnel) msg)
        (recur)))))

(defn emulate-device []
  (let [name (pubnub/generate-id)]
    (go-loop []
      (pubnub/bidir-send (pubnub/tunnel)
                         {:id name
                          :value (str (pubnub/generate-id))})
      (<! (timeout (* (rand-int 3) 1000)))
      (recur))))

(defroute device "/device" []
  (js/console.log "Emulate Device")
  (emulate-device)
  (aset js/window "location" (str "/#")))

(defn static-page []
  (let [out (chan 1)]
    (go
      (put! out
            (-> @devices-var
                (monitor-page :scripts scripts)
                (reagent/render-to-string)
                (html5))))
    out))

(defn activate []
  (let [el (dom/getElement "main")
        h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))
    (reagent/render [#(monitor-view @devices-var)] el)
    (track-devices)))
