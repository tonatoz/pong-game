(ns pong-game.game)

(def field {:w 100 :h 100})
(def ball-radius 2)
(def platform {:w 3 :h 10})

(defn new-game [left-user right-user]
	(agent {
		:left {
			:user left-user
			:y (+ (/ platform-lenght 2) (/ (:h field) 2))}
		:right {
			:user right-user
			:y (+ (/ platform-lenght 2) (/ (:h field) 2))}
		:ball {
			:x (/ (:w field) 2)
			:y (/ (:y field) 2)
			:x-speed 25
			:y-speed 25}}))

(defn check-ball [state]
	(let [top (+ ball-radius (-> @state :ball :y))
				bottom (- (-> @state :ball :y) ball-radius)
				left (- (-> @state :ball :x) ball-radius)
				right (+ ball-radius (-> @state :ball :x))
				on-platform? (fn [y] (and (=> top y) (=< bottom (+ y (:h platform)))))]
		(cond 
			(<= 0 left) "win right"
			(>= (:w field) right) "win left"
			(or
				(<= 0 top) 
				(>= (:h field) bottom)) (send state update-in [:ball :y-speed] * -1)
			(or 
				(and
					(<= (:w platform) left)
					(on-platform? (-> @state :left :y)))
				(and
					(>= (- (:w field) (:w platform)) right)
					(on-platform? (-> @state :right :y)))) (send state update-in [:ball :x-speed] * -1)
			:else nil)))

(defn move [state]
	(send state 
		#(do
			(update-in % [:ball :x] + (-> @state :ball :x-speed))
			(update-in % [:ball :y] + (-> @state :ball :y-speed)))))

(defn game-loop []
	(loop []
		(Thread/sleep 100)
		(let [check-result (check-ball ??)]
			(move ??)
			(when check-result
				(recur)))))