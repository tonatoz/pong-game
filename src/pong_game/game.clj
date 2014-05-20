(ns pong-game.game
	(:require [org.httpkit.server :refer [send!]]
						[cheshire.core :refer [generate-string]]))

(def field {:w 500 :h 400})
(def ball-radius 10)
(def platform {:w 5 :h 75})

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
			:y-speed 1}
		:status :run}))

(defn say [state action params]
	(let [body (generate-string {:method action :params params})]
		(send! (-> @state :left :user :channel) body)
		(send! (-> @state :right :user :channel) body)))

(defn check-end-of-game [state]
	(let [left (- (-> @state :ball :x) ball-radius)
				right (+ (-> @state :ball :x) ball-radius)]
		(cond 
			(<= left 0) "win right"
			(>= right (:w field)) "win left"
			:else nil)))

(defn process-ball [state]
	(let [top (- (-> @state :ball :y) ball-radius)
				bottom (+ (-> @state :ball :y) ball-radius)
				left (- (-> @state :ball :x) ball-radius)
				right (+ (-> @state :ball :x) ball-radius)
				on-platform? (fn [y] (and (>= top y) (<= bottom (+ y (:h platform)))))]
		(cond 
			(or
				(<= top 0) 
				(>= bottom (:h field))) (send state update-in [:ball :y-speed] * -1)
			(or 
				(and
					(<= left (:w platform))
					(on-platform? (-> @state :left :y)))
				(and
					(>= right (- (:w field) (:w platform)))
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
			(say game "game-end" game-result)
			(send game assoc-in :status :stop))
		(do 
			(process-ball game)
			(move game))))

(defn run-game [game]
	(future
		(loop []
			(when (not= :stop (:status @game)) 
				(game-tick game)
				(Thread/sleep 10)
				(recur)))))