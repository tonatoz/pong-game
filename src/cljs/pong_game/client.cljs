(ns pong-game.core
  (:require [enfocus.core :as ef]
            [enfocus.events :as ev])
  (:require-macros [enfocus.macros :as em]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utils
;;
(defn log [msg]
  (.log js/console msg))

(defn toJSON [o]
  (let [o (if (map? o) (clj->js o) o)]
    (.stringify (.-JSON js/window) o)))

(defn parseJSON [x]
  (js->clj 
    (.parse (.-JSON js/window) x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User action & events
;;
(defn signin-handler [{:keys [status user-list text]}]
  (if (= "ok" status)
    (ef/at ".container" (ef/content (lobby-snippet user-list)))
    (js/alert (str "Login error: " text))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WebSocket
;;
(def ws (js/WebSocket. "ws://localhost:8080/ws"))

(defn send-ws [data]
  (.send ws (toJSON data)))

(set! (.-oncose ws) #(js/alert "Потеряно соединение с сервером"))

(set! (.-onmessage ws) 
  (fn [event]
    (let [data (parseJSON (.-data event))]
      (case (:method data)
        "event-signin" (signin-handler data)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Snippets
;;
(em/defsnippet signin-snippet "/template.html" "#signin" []
  "form" (ev/listen :submit 
    (fn [event]
      (send-ws {
        :method "action-signin"
        :login (ef/from "input[name='username']" (ef/read-form-input))})
      (.preventDefault event)
      (.stopPropagation event))))

(em/defsnippet lobby-snippet "/template.html" "#lobby" [user-list]
  "tbody" )

(em/defsnippet game-snippet "/template.html" "#game" [])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start
;;
(defn start []
  (ef/at ".container" (ef/content (signin-snippet))))

(set! (.-onload js/window) #(em/wait-for-load (start)))