(ns pong-game.game
	(:requier [org.httpkit.server :refer [send!]]
						[cheshire.core :refer [generate-string]]))

(def field {:w 100 :h 100})
(def ball-radius 2)
(def platform {:w 3 :h 10})

(defn new-game [left-user right-user]
	(agent {
		:left {
			:user left-user
			:y (+ (/ (:w platform) 2) (/ (:h field) 2))}
		:right {
			:user right-user
			:y (+ (/ (:w platform) 2) (/ (:h field) 2))}
		:ball {
			:x (/ (:w field) 2)
			:y (/ (:y field) 2)
			:x-speed 25
			:y-speed 25}}))

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
	(send state 
		#(do
			(update-in % [:ball :x] + (-> @state :ball :x-speed))
			(update-in % [:ball :y] + (-> @state :ball :y-speed))
			(say state "ball-move" {:x (-> @state :ball :x) :y (-> @state :ball :y)}))))

(defn platform-move [state ch-id y]
	(send state assoc-in [(if (= ch-id (-> @state :left :channel)) :left :right) :y] y))

(defn game-tick [game]
	(if-let [game-result (check-end-of-game game)]
		(say game "game-end" game-result)
		(do 
			(process-ball game)
			(move game))))

(defn run-game [game]
  (future
    (doseq [f (repeatedly #(game-tick game))]
      (Thread/sleep 100)
      (f))))