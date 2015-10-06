(ns app.views
  (:require
   [reagent.core :as reagent :refer [atom]]
   [kioo.reagent :refer [html-content content append after set-attr do->
                         substitute listen unwrap]]
   [kioo.core :refer [handle-wrapper]]
   [goog.string :as gstring])
  (:require-macros
   [kioo.reagent :refer [defsnippet deftemplate snippet]]))

(defn counter [device s]
  (if (not= :expired (:state device))
    [:span.badge.pull-right
     (case (:state device)
       nil
       [:span.glyphicon.glyphicon-ok-circle]
       :warn
       [:span.glyphicon.glyphicon-exclamation-sign]
       :alarm
       [:span.glyphicon.glyphicon-earphone])
     (str " " s) ]))

(defn device-card [device]
  ^{:key (gstring/hashCode (pr-str device))}
  [:div.card.col-xs-12.col-sm-6.col-md-4.col-lg-3
    [:div.panel.panel-primary
     [:div.panel-heading
      [:span.device-label (str (:id device))]
      [counter device (:counter device)]]
     (into [:table.table-striped]
           (for [[label value] (dissoc device :id :counter :state)]
             [:tr
              [:th (name label)]
              [:td (str value)]])) ]])

(defsnippet monitor-view "template.html" [:main :.row]
  [devices-map]
  {[:.card]
   (substitute
    (->> devices-map
         (map second)
         (sort-by :id)
         (map device-card))) })

(defsnippet monitor-page "template.html" [:html]
  [devices & {:keys [scripts]}]
  {[:main] (content [monitor-view devices])
   [:body] (append
             [:div (for [src scripts]
                     ^{:key (gstring/hashCode (pr-str src))}
                     [:script src])]) })

(defn html5 [content]
  (str "<!DOCTYPE html>\n" content))
