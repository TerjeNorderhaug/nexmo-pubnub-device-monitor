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
   [app.debug :refer [echo]]
   [app.json :refer [fetch-json]]
   [app.views :refer [monitor-view monitor-page html5]]
   [app.pubnub :as pubnub])
  (:import goog.History))

(secretary/set-config! :prefix "#")

(defonce devices-var (atom {}))

(defn monitor-devices [in]
  (go-loop []
    (when-let [msg (<! in)]
      (pubnub/bidir-send (pubnub/tunnel) msg)
      (recur))))

(defn emulate-device []
  (let [name (pubnub/generate-id)]
    (go-loop []
      (pubnub/bidir-send (pubnub/tunnel)
                         {:id name
                          :value (str (pubnub/generate-id))})
      (<! (timeout (* (if (zero? (rand-int 5)) (rand-int 90)(rand-int 15))
                      1000)))
      (recur))))

(defroute device "/device" []
  (js/console.log "Emulate Device")
  (emulate-device)
  (aset js/window "location" (str "/#")))

(defn expired-devices [expiration]
  (filter
   (fn [[_ device]]
     (< (:utime device) expiration))
   @devices-var))

(defn track-devices [& [alarm]]
  (let [in (:in-chan (pubnub/tunnel))]
    (go-loop []
      (when-let [val (<! in)]
        (swap! devices-var
               #(update % (:id val) (fn [_] val)))
        (let [utime (<! (pubnub/fetch-time))]
          (doseq [[_ device] (expired-devices (- utime (* 60 1000)))]
            (println "[EXPIRED]" device (- utime (* 60 1000)) (:utime device))))
        (recur)))))

(defn activate []
  (let [el (dom/getElement "main")
        h (History.)
        utime (atom nil)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))
    (go-loop []
      (when-let [u (<! (pubnub/fetch-time))]
        (reset! utime u)
        (<! (timeout 40))
        (recur)))
    (go-loop [devices (<! (fetch-json "/devices"))]
      (reset! devices-var
              (into {} (map #(vector (:id %) %)) devices))
      (reagent/render [#(monitor-view @devices-var @utime)] el)
      (track-devices))))

(defn active-devices []
  (let [out (chan 1)]
    (go-loop [utime (<! (pubnub/fetch-time))]
      (->>
       @devices-var
       (filter (fn [[id dev]]
                 [id (> (:utime dev)
                        (- utime (* 600 1000)))]))
       (#(or % []))
       (into {})
       (put! out)))
    out))

(defn scripts []
  [{:src "/js/out/app.js"}
   "main_cljs_fn()"])

(defn static-page []
  (let [out (chan 1)]
    (go
      (put! out
            (-> (<! (active-devices))
                (monitor-page
                 :utime (<! (pubnub/fetch-time))
                 :scripts (scripts))
                (reagent/render-to-string)
                (html5))))
    out))
