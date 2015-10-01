(ns app.views
  (:require
   [reagent.core :as reagent :refer [atom]]
   [kioo.reagent :refer [html-content content append after set-attr do->
                         substitute listen unwrap]]
   [kioo.core :refer [handle-wrapper]]
   [goog.string :as gstring])
  (:require-macros
   [kioo.reagent :refer [defsnippet deftemplate snippet]]))

(defn device-card [[id device]]
  ^{:key (gstring/hashCode (pr-str device))}
  [:div.card.col-xs-12.col-sm-6.col-md-4.col-lg-3
    [:div.panel.panel-primary
     [:div.panel-heading (str id)]
     (into [:table]
           (for [[label value] device]
             [:tr
              [:th (name label)]
              [:td (str value)]])) ]])

(defsnippet monitor-view "template.html" [:main :.row]
  [devices]
  {[:.card] (substitute (map device-card (sort-by first devices))) })

(defsnippet monitor-page "template.html" [:html]
  [devices & {:keys [scripts]}]
  {[:main] (content [monitor-view devices])
   [:body] (append [:div (for [src scripts]
                           ^{:key (gstring/hashCode (pr-str src))}
                           [:script src])]) })

(defn html5 [content]
  (str "<!DOCTYPE html>\n" content))
