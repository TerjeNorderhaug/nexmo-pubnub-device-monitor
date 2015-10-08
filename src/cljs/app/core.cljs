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

(def expiration (* 30 1000))

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
  (emulate-device)
  (aset js/window "location" (str "/#")))

(defn decide-state [device utime]
  (cond
    (not (:utime device))
    nil
    (< (:utime device)
       (- utime (* 600 1000)))
    :expired
    (< (:utime device)
       (- utime expiration))
    :alarm
    (< (:utime device)
       (- utime (* 10 1000)))
    :warn))

(defn track-devices [& [alarm]]
  (let [in (:in-chan (pubnub/tunnel))]
    (go-loop [active-devices {}]
      (when-let [msg (<! in)]
        (swap! devices-var
               #(assoc % (:id msg) msg))
        (let [utime (<! (pubnub/fetch-time))
              expires (- utime expiration)
              expire? #(< % expires)]
          (doseq [[id recent] active-devices
                  :when (expire? recent)]
            #_ (if alarm (alarm))
            (println "[EXPIRED]" id expires recent))
          (recur (assoc (apply dissoc active-devices
                               (remove (comp expire? second)
                                       active-devices))
                        (:id msg) utime) ))))))

(defn devices-cursor
  ([devices utime]
   (->>
    devices
    (map (fn [[id device]]
           [id (assoc device
                      :state (decide-state device utime)
                      :counter (if (and utime (not= 0 utime))
                                 (quot
                                  (- utime (:utime device))
                                  1000))) ]))))
  ([utime]
   (devices-cursor @devices-var utime)))

(defn activate []
  (let [el (dom/getElement "main")
        h (History.)
        utime (atom nil)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))
    (go-loop []
      (when-let [u (<! (pubnub/fetch-time))]
        (reset! utime u)
        (<! (timeout 300))
        (recur))
      (println "[PUBNUB] time stopped"))
    (go-loop [devices (->> (<! (fetch-json "/devices"))
                           (map #(vector (:id %) %))
                           (into {}))]
      (reset! devices-var devices)
      (reagent/render [#(monitor-view (devices-cursor @%1 @%2)) devices-var utime] el)
      (track-devices))))

(defn active-devices [utime]
  (->>
   @devices-var
   (filter
    (fn [[id dev]]
      (not= :expired (decide-state dev utime))))
   (#(or % []))
   (into {})))

(defn scripts []
  [{:src "/js/out/app.js"}
   "main_cljs_fn()"])

(defn static-page []
  (let [out (chan 1)]
    (go-loop [utime (<! (pubnub/fetch-time))
              devices (active-devices utime)]
      (put! out
            (-> (devices-cursor devices utime)
                (monitor-page
                 :scripts (scripts))
                (reagent/render-to-string)
                (html5))))
    out))
