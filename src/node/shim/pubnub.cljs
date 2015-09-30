(ns shim.pubnub
  (:require
   [cljs.nodejs :as nodejs]))

(def pubnub (nodejs/require "pubnub"))
(set! js/PUBNUB pubnub) ;; likely not needed?
