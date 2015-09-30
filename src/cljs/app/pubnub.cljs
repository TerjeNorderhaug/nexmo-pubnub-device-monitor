(ns app.pubnub
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [app.env :refer [env environment]])
  (:require
   [cljs.core.async :refer [chan <! >! put! timeout]]
   [shim.pubnub :refer [pubnub]]
   [app.debug :refer [echo]]))

(defn open-pubnub [options]
  (pubnub (clj->js options)))

(def connected (atom false))

(defn connected? []
  @connected)

(defn on-connect []
  (println "[PUBNUB] Connected")
  (reset! connected true))

(defn on-disconnect []
  (println "[PUBNUB] Disconnected")
  (reset! connected false))

(defn generate-id []
  (-> (.random js/Math)
      (.toString 36)
      (.substring 7)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; BIDIRECTIONAL CHANNEL (with browser)

(def publish-key (env :pubnub-publish-key))
(def subscribe-key (env :pubnub-subscribe-key))

(def bidir-options {:keepalive 5
                    :restore true
                    :ssl true ;; need to be conditonal for browser
                    :publish_key publish-key
                    :subscribe_key subscribe-key
                    })

;; ## need to block when channel is full!!
;; ## Also make sure tap doesn't drain the initial stream values
;; ## Should use different names for the different directions!

(defn pubnub-bidir [pubnub name {:keys [restore backlog] :as options}]
  (.unsubscribe pubnub (clj->js {:channel name}))
  (let [out-chan (chan)
        in-chan (chan)
        errfn #(println "[PUBNUB] Error: " (.-error %))
        on-message (fn [message [_ time-token] channel]
                     (put! in-chan
                           (assoc
                            (js->clj message :keywordize-keys true)
                            :utime (quot time-token 10000))))]
    (.time
     pubnub
     (fn [utime]
       (.subscribe pubnub
                   (clj->js
                    {:channel name
                     :message on-message
                     :connect on-connect
                     :reconnect on-connect
                     :disconnect on-disconnect
                     :error errfn
                     :restore (boolean restore)
                     :timetoken (if backlog
                                  (str (- utime (* backlog 10000000))))
                     }))))
    (go-loop []
      (let [message (<! out-chan)]
        (.publish pubnub
                  (clj->js
                   {:channel name
                    :message message}))
        (recur)))
    {:in-chan in-chan
     :out-chan out-chan}))

(defn bidir-send [bidir msg]
  (put! (:out-chan bidir) msg))

(defn browser-bidir []
  (pubnub-bidir (open-pubnub bidir-options) "arrive" {}))

(def tunnel
  (memoize #(pubnub-bidir (open-pubnub bidir-options) "arrive" {})))

(defn keep-updated [atom-var chan & [f]]
  (go-loop []
    (when-let [val (<! chan)]
      (reset! atom-var
              (if f (f @atom-var val) val))
      (recur))))

;; (bidir-send (tunnel) {:id "hello2"})
;; (echo (:in-chan (tunnel)))
