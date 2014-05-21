(ns pong-game.game
	(:require [org.httpkit.server :refer [send!]]
						[cheshire.core :refer [generate-string]]))

(def field {:w 729 :h 537})
(def ^Integer ball-radius 5)
(def platform {:w 5 :h 150})

(defn new-game [left-user right-user]
	(agent {
		:left {
			:user left-user
			:x 0
			:y (- (/ (:h field) 2) (/ (:h platform) 2))}
		:right {
			:user right-user
			:x (- (:w field) (:w platform))
			:y (- (/ (:h field) 2) (/ (:h platform) 2))}
		:ball {
			:x 50
			:y 50
			:x-speed 8
			:y-speed 4}
		:status true} :error-mode :continue))

(defn- say [state action params]
	(let [body (generate-string {:method action :params params})]
		(when-let [ch (-> @state :left :user :channel)]
			(send! ch body))
		(when-let [ch (-> @state :right :user :channel)]
			(send! ch body))))

(defn platform-move [state ch-id y]
	(let [side (if (= ch-id (hash (-> @state :left :user :channel))) :left :right)]		
		(when (and (>= y 0) (<= y (- (:h field) (:h platform))))	
			(send state assoc-in [side :y] y)
			(say state "platform-move" {:side side :y y}))))

(defn- check-end-of-game [state]
	(let [left (- (-> @state :ball :x) ball-radius)
				right (+ (-> @state :ball :x) ball-radius)]
		(or
			(< left 0) 
			(> right (:w field)))))

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
				(and (>= bx (- px (:w platform)))
						 (> px 0))
				(and (<= bx (:w platform))
					   (= px 0))))))

(defn- process-ball [state]
	(let [top (- (-> @state :ball :y) ball-radius)
				bottom (+ (-> @state :ball :y) ball-radius)]
		(when (< top 0) 
			(send state update-in [:ball :y-speed] * -1)
			(send state assoc-in [:ball :y] ball-radius))
		(when (> bottom (:h field)) 
			(send state update-in [:ball :y-speed] * -1)
			(send state assoc-in [:ball :y] (- (:h field) ball-radius)))))

(defn- ball-move [state]
	(send state update-in [:ball :x] + (-> @state :ball :x-speed))
	(send state update-in [:ball :y] + (-> @state :ball :y-speed))
	(say state "ball-move" {:x (-> @state :ball :x) :y (-> @state :ball :y)}))

(defn- update [game]
	(when (or (check-paltform-collision game :left)
						(check-paltform-collision game :right))
		(send game update-in [:ball :x-speed] * -1))
	(if (check-end-of-game game)
		(do
			(say game "game-end" {:text "thank you!"})
				(send game assoc-in [:status] false))
		(do
			(process-ball game)	
			(ball-move game))))

(defn run-game [game]
	(future
		(loop []
			(if (:status @game)
				(do
					(update game)
					(Thread/sleep (/ 1000 60))
					(recur))
				(println "Thread stop")))))