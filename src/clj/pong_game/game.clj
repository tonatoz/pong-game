(ns pong-game.game
	(:require [org.httpkit.server :refer [send!]]
						[cheshire.core :refer [generate-string]]))

(def field {:w 729 :h 537})
(def ball-radius 5)
(def platform {:w 5 :h 150})

(defn new-game [left-user right-user]
	(atom {
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
		:status true}))

(defn- say [state action params]
	(let [body (generate-string (merge {:method action} params))]
		(when-let [ch (-> @state :left :user :channel)]
			(send! ch body))
		(when-let [ch (-> @state :right :user :channel)]
			(send! ch body))))

(defn stop-game [state]
	(say state "event-game-end" {:text "thank you!"})
	(swap! state assoc-in [:status] false))

(defn platform-move [state ch-id y]
	(let [side (if (= ch-id (hash (-> @state :left :user :channel))) :left :right)]		
		(when (and (>= y 0) (<= y (- (:h field) (:h platform))))	
			(swap! state assoc-in [side :y] y)
			(say state "event-platform-move" {:side side :y y}))))

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
	(let [top (- (-> @state :ball :y) ball-radius)
				bottom (+ (-> @state :ball :y) ball-radius)]
		(when (< top 0) 
			(swap! state update-in [:ball :y-speed] * -1)
			(swap! state assoc-in [:ball :y] ball-radius))
		(when (> bottom (:h field)) 
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
		(loop []
			(when (:status @game)
				(update game)
				(Thread/sleep 10)
				(recur)))))