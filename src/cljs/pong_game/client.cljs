(ns pong-game.core
  (:require [enfocus.core :as ef]
            [enfocus.events :as ev])
  (:require-macros [enfocus.macros :as em]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utils
;;
(defn log [msg]
  (.log js/console msg))

(defn to-json [o]
  (let [o (if (map? o) (clj->js o) o)]
    (.stringify (.-JSON js/window) o)))

(defn parse-json [x]
  (js->clj 
    (.parse (.-JSON js/window) x) :keywordize-keys true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game objects
;;
(def W 729) 
(def H 537)

(def left  (atom {:w 5, :h 150, :y (- (/ W 2) (/ 5 2)), :x 0}))
(def right (atom {:w 5, :h 150, :y (- (/ W 2) (/ 5 2)), :x (- W  5)}))
(def ball  (atom {:x 50, :y 50, :r 5}))

(defn- draw-canvas [ctx]
  (set! (.-fillStyle ctx) "black")
  (.fillRect ctx 0 0 W H))

(defn- draw-platfiorm [ctx side]
  (set! (.-fillStyle ctx) "white")
  (.fillRect ctx (:x @side) (:y @side) (:w @side) (:h @side)))

(defn- draw-ball [ctx]
  (.beginPath ctx)
  (set! (.-fillStyle ctx) "white")
  (.arc ctx (:x @ball) (:y @ball) (:r @ball) 0 (* js/Math.PI 2) false)
  (.fill ctx))

(defn draw []
  (let [canvas (.getElementById js/document "canvas")
        ctx (.getContext canvas "2d")]
    (draw-canvas ctx)
    (draw-platfiorm ctx left)
    (draw-platfiorm ctx right)
    (draw-ball ctx)))

(defn game-print [text]
  (let [canvas (.getElementById js/document "canvas")
        ctx (.getContext canvas "2d")]   
    (.clearRect ctx 0 0 W H) 
    (set! (.-fillStlye ctx) "white")
    (set! (.-font ctx) "25px Arial, sans-serif")
    (set! (.-textAlign ctx) "center")
    (set! (.-textBaseline ctx) "middle")
    (.fillText ctx text (/ W 2) (/ H 2))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User action & events
;;
(declare lobby-snippet game-snippet user-tbl-row send-ws)

(defn signin-handler [{:keys [status user-list text]}]
  (if (= "ok" status)
    (ef/at ".container" (ef/content (lobby-snippet user-list)))
    (js/alert (str "Ошибка входа: " text))))

(defn new-user-handler [user]
  (ef/at "tbody" (ef/append (user-tbl-row user))))

(defn start-game [{:keys [users]}]
  (ef/at ".container" (ef/content (game-snippet users))))

(defn platform-move [{:keys [side y]}]
  (if (= side "left")
    (swap! left assoc-in [:y] y)
    (swap! right assoc-in [:y] y))
  (draw))

(defn ball-move [{:keys [x y]}]
  (swap! ball assoc-in [:x] x)
  (swap! ball assoc-in [:y] y)
  (draw))

(defn end-of-game [text]
  (js/alert text)
  (send-ws "action-game-end"))

(defn update-score [{:keys [left-score right-score]}]
  (ef/at "#left-score" (ef/content (str left-score)))
  (ef/at "#right-score" (ef/content (str right-score))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WebSocket
;;
(def ws 
  (js/WebSocket. (str "ws://" (.-host (.-location js/window)) "/ws")))

(defn send-ws [method & [params]]
  (.send ws (to-json (merge {:method method} params))))

(set! (.-onclose ws) #(js/alert "Потеряно соединение с сервером"))

(set! (.-onmessage ws) 
  (fn [event]
    (let [data (parse-json (.-data event))]
      (case (:method data)
        "event-signin" (signin-handler data)
        "event-new-user" (new-user-handler data)
        "event-rem-user" (ef/at [:tbody [:tr {:data-id (:id data)}]] (ef/remove-node))
        "event-fight" (start-game data)
        "event-start-after" (game-print (str "Игра начнеться через: " (:after data)))
        "event-platform-move" (platform-move data)
        "event-ball-move" (ball-move data)
        "event-update-score" (update-score data)
        "event-game-end" (end-of-game (:text data))
        (log (str "Неизвестный метод: " (:method data)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Snippets
;;
(em/defsnippet signin-snippet "/template.html" "#signin" []
  "form" (ev/listen :submit 
    (fn [event]
      (.preventDefault event)
      (send-ws "action-signin" {
        :login (:username (ef/from "form" (ef/read-form)))}))))

(defn user-tbl-row [{:keys [id name]}]
 (ef/html 
    [:tr 
      [:td name]
      [:td [:button.fight-btn.btn.btn-primary {:data-id id} "Сразиться"]]]))

(em/defsnippet lobby-snippet "/template.html" "#lobby" [user-list]
  "tbody" (apply ef/append (map user-tbl-row user-list))
  "table" (ev/listen-live :click ".fight-btn"
    (fn [event]
      (send-ws "action-start-fight" {
        :with (ef/from (.-target event) (ef/get-attr "data-id"))}))))

(em/defsnippet game-snippet "/template.html" "#game" [[left-user right-user]]
  "#left-user" (ef/content left-user)
  "#right-user" (ef/content right-user)
  "#canvas" (ev/listen :mousemove 
    (fn [event]
      (let [canvas (.getElementById js/document "canvas")
            top (.-top (.getBoundingClientRect canvas))
            mouse-y (.-clientY event)]
        (send-ws "action-move" {
          :pos (- mouse-y top)})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start
;;
(defn start []
  (ef/at ".container" (ef/content (signin-snippet))))

(set! (.-onload js/window) #(em/wait-for-load (start)))