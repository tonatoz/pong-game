(ns pong-game.game
	(:require [org.httpkit.server :refer [send!]]
						[cheshire.core :refer [generate-string]]))

(def field {:w 729 :h 537})
(def ball-radius 5)
(def ball-init {:x 350 :y 150 :x-speed 8 :y-speed 4})
(def platform {:w 5 :h 150})
(def stop-score 10)

(defn new-game [left-user right-user]
	(atom {
		:left {
			:user left-user
			:score 0
			:x 0
			:y (- (/ (:h field) 2) (/ (:h platform) 2))}
		:right {
			:user right-user
			:score 0
			:x (- (:w field) (:w platform))
			:y (- (/ (:h field) 2) (/ (:h platform) 2))}
		:ball ball-init
		:fps 20					 ;; Таймаут обработчика полета шарика 
		:status false})) ;; Работает ли игра сйечас

(defn- say [state action params]
	(let [body (generate-string (merge {:method action} params))]
		(send! (-> @state :left :user :channel) body)
		(send! (-> @state :right :user :channel) body)))

(defn stop-game [state]
	(say state "event-game-end" {:text "Спасибо за игру"})
	(swap! state assoc-in [:status] false))

(defn platform-move [state ch-id y]
	(when (:status @state)
		(let [side (if (= ch-id (hash (-> @state :left :user :channel))) :left :right)]		
			(when (and (>= y 0) (<= y (- (:h field) (:h platform))))	
				(swap! state assoc-in [side :y] y)
				(say state "event-platform-move" {:side side :y y})))))

(defn- update-score [state side-win]
	(when (odd? (-> @state side-win :score))
		(swap! state update-in [:fps] dec))	
	(swap! state assoc-in [:ball] ball-init)
	(swap! state update-in [side-win :score] inc)
	(say state "event-update-score" {
		:left-score (-> @state :left :score)
		:right-score (-> @state :right :score)})
	(>= (-> @state side-win :score) stop-score))

(defn- check-end-of-game [state]
	(let [b-left (- (-> @state :ball :x) ball-radius)
				b-right (+ (-> @state :ball :x) ball-radius)]
		(cond
			(< b-left 0) (update-score state :right)
			(> b-right (:w field)) (update-score state :left))))

(defn- check-paltform-collision [state side]
	(let [bx (-> @state :ball :x)
				by (-> @state :ball :y)
				px (-> @state side :x)
				py (-> @state side :y)]
		(and 
			(and 
				(>= (- by ball-radius) py)
		 		(<= (+ by ball-radius) (+ py (:h platform))))
			(or
				(and (>= bx px)
						 (> px 0))
				(and (<= bx (:w platform))
					   (= px 0))))))

(defn- process-platform-collision [state]
	(when (check-paltform-collision state :left)
		(swap! state update-in [:ball :x-speed] * -1)
		(swap! state assoc-in [:ball :x] (+ (:w platform) ball-radius)))
	(when (check-paltform-collision state :right)
		(swap! state update-in [:ball :x-speed] * -1)
		(swap! state assoc-in [:ball :x] (- (:w field) (:w platform) ball-radius))))

(defn- process-ball [state]
	(let [b-top (- (-> @state :ball :y) ball-radius)
				b-bottom (+ (-> @state :ball :y) ball-radius)]
		(when (< b-top 0) 
			(swap! state update-in [:ball :y-speed] * -1)
			(swap! state assoc-in [:ball :y] ball-radius))
		(when (> b-bottom (:h field)) 
			(swap! state update-in [:ball :y-speed] * -1)
			(swap! state assoc-in [:ball :y] (- (:h field) ball-radius)))))

(defn- ball-move [state]
	(swap! state update-in [:ball :x] + (-> @state :ball :x-speed))
	(swap! state update-in [:ball :y] + (-> @state :ball :y-speed))
	(say state "event-ball-move" {:x (-> @state :ball :x) :y (-> @state :ball :y)}))

(defn- update [game]
	(process-platform-collision game)
	(if (check-end-of-game game)
		(stop-game game)
		(do
			(process-ball game)	
			(ball-move game))))

(defn run-game [game]
	(future
		(doseq [i (reverse (range 4))]			
			(say game "event-start-after" {:after i})
			(Thread/sleep 1000))
		(swap! game assoc-in [:status] true)
		(loop []
			(when (:status @game)
				(update game)
				(Thread/sleep (:fps @game))
				(recur)))))