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
  (if (<= 0 s 9999)
    [:span.badge.pull-right
     (cond
       (< s 10)
       [:span.glyphicon.glyphicon-ok-circle]
       (< s 60)
       [:span.glyphicon.glyphicon-exclamation-sign]
       true
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
           (for [[label value] (dissoc device :id :counter)]
             [:tr
              [:th (name label)]
              [:td (str value)]])) ]])

(defsnippet monitor-view "template.html" [:main :.row]
  [devices-map utime]
  {[:.card]
   (substitute
    (->> devices-map
         (map second)
         (sort-by :id)
         (map (fn [device]
                (assoc device
                       :counter (if utime
                                  (quot
                                   (- utime (:utime device))
                                   1000)))))
         (map device-card))) })

(defsnippet monitor-page "template.html" [:html]
  [devices & {:keys [scripts utime]}]
  {[:main] (content [monitor-view devices utime])
   [:body] (append
             [:div (for [src scripts]
                     ^{:key (gstring/hashCode (pr-str src))}
                     [:script src])]) })

(defn html5 [content]
  (str "<!DOCTYPE html>\n" content))
