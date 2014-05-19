(ns pong-game.game
	(:require [org.httpkit.server :refer [send!]]
						[cheshire.core :refer [generate-string]]))

(def field {:w 100 :h 100})
(def ball-radius 2)
(def platform {:w 4 :h 10})

(defn new-game [left-user right-user]
	(agent {
		:left {
			:user left-user
			:y (- (/ (:h field) 2) (/ (:h platform) 2))}
		:right {
			:user right-user
			:y (- (/ (:h field) 2) (/ (:h platform) 2))}
		:ball {
			:x (/ (:w field) 2)
			:y (/ (:h field) 2)
			:x-speed 1
			:y-speed 1}}))

(defn say [state action params]
	(let [body (generate-string {:method action :params params})]
		(send! (-> @state :left :user :channel) body)
		(send! (-> @state :right :user :channel) body)))

(defn check-end-of-game [state]
	(let [left (- (-> @state :ball :x) ball-radius)
				right (+ ball-radius (-> @state :ball :x))]
		(cond 
			(<= 0 left) "win right"
			(>= (:w field) right) "win left")
			:else nil))

(defn process-ball [state]
	(let [top (+ ball-radius (-> @state :ball :y))
				bottom (- (-> @state :ball :y) ball-radius)
				left (- (-> @state :ball :x) ball-radius)
				right (+ ball-radius (-> @state :ball :x))
				on-platform? (fn [y] (and (>= top y) (<= bottom (+ y (:h platform)))))]
		(cond 
			(or
				(<= 0 top) 
				(>= (:h field) bottom)) (send state update-in [:ball :y-speed] * -1)
			(or 
				(and
					(<= (:w platform) left)
					(on-platform? (-> @state :left :y)))
				(and
					(>= (- (:w field) (:w platform)) right)
					(on-platform? (-> @state :right :y)))) (send state update-in [:ball :x-speed] * -1))))

(defn move [state]
	(send state update-in [:ball :x] + (-> @state :ball :x-speed))
	(send state update-in [:ball :y] + (-> @state :ball :y-speed))
	(println "Coord: " {:x (-> @state :ball :x) :y (-> @state :ball :y)})
	(say state "ball-move" {:x (-> @state :ball :x) :y (-> @state :ball :y)}))

(defn platform-move [state ch-id y]
	(let [side (if (= ch-id (hash (-> @state :left :user :channel))) :left :right)]
		(send state assoc-in [side :y] y)
		(say state "platform-move" {:side side :y y})))

(defn game-tick [game]
	(if-let [game-result (check-end-of-game game)]
		(do
			(println "game end: " game-result) 
			(say game "game-end" game-result))
		(do 
			(process-ball game)
			(move game))))

(defn run-game [game]
	(future
		(loop []
			(game-tick game)
			(Thread/sleep 1000)
			(when game 
				(recur)))))