(ns app.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :as async :refer [chan close! timeout put!]]
   [goog.dom :as dom]
   [goog.events :as events]
   [reagent.core :as reagent :refer [atom]]
   [app.views :refer [monitor-view monitor-page html5]]
   [app.pubnub :as pubnub]))

(def scripts [{:src "/js/out/app.js"}
              "main_cljs_fn()"])

(defonce devices-var (atom {}))

(defn track-devices []
  (go-loop []
    (when-let [val (<! (:in-chan (pubnub/tunnel)))]
      (reset! devices-var
              (update @devices-var (:id val) (fn [_] val)))
      (println ">>>>" val)
      (recur))))

(defn emulate-devices [n]
  (go-loop []
    (pubnub/bidir-send (pubnub/tunnel)
                       {:id (str "device-" (inc (rand-int n)))
                        :value (str (pubnub/generate-id))})
    (<! (timeout (* (rand-int 3) 1000)))
    (recur)))

(defn monitor-devices [& {:keys [alarm]}]
  (emulate-devices 12)
  (when alarm
    (println "ALARM!"))
  (track-devices))

(defn activate []
  (let [el (dom/getElement "main")]
    (reagent/render [#(monitor-view @devices-var)] el)
    (track-devices)))

(defn static-page []
  (let [out (chan 1)]
    (go
      (put! out
            (-> @devices-var
                (monitor-page :scripts scripts)
                (reagent/render-to-string)
                (html5))))
    out))
