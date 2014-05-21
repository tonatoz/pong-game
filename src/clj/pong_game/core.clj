(ns pong-game.core
  (:require [org.httpkit.server :refer :all]
  					[compojure.core :refer [defroutes GET]]
  					[compojure.handler :refer [site]]
  					[compojure.route :refer [resources not-found]]
  					[ring.middleware.reload :refer [wrap-reload]]
  					[ring.util.response :refer [resource-response]]
  					[cheshire.core :as json]
  					[pong-game.game :as game]))

;; item -> {channel-id {:name user-name :channel user-channel}}
(def lobby (atom {})) 
;; item -> {user-channel (ref: game/user-game)}
(def game (atom {})) 

(defn- send-all [data]
	(doseq [ch (map :channel (vals @lobby))]
		(send! ch (json/generate-string data))))

(defn- lobby-exit [channel-id]
  (swap! lobby dissoc channel-id)
  (send-all {:method "rem-user" :id channel-id}))

(defn- user-exit [channel-id]
	(lobby-exit channel-id)
  (when-let [game-agent (get @game channel-id)]
    (send game-agent assoc-in [:status] false))
	(swap! game dissoc channel-id))

(defn signin [username channel]
	(if (empty? (filter #(= username (:name %)) @lobby))
		(do 
			(send-all {
				:method "add-user" 
				:name username 
				:id (hash channel)})
			(swap! lobby assoc (hash channel) {:name username :channel channel})
			(send! channel {
        :method "event-signin" 
        :status "ok" 
        :text "Вход выполнен спешно" 
        :user-list []}))
		(send! channel {
      :method "event-signin" 
      :status "fail" 
      :text "такое имя уже занято" 
      :user-list []})))

(defn user-start-fight [me-id op-id]
	(if-let [[me op] (map #(get @lobby %) [me-id op-id])]
		(let [game-state (game/new-game me op)]
			(doseq [p [me op]]
				(send! (:channel p) (json/generate-string {:method "fight"}))
        (lobby-exit (hash (:channel p)))
				(swap! game assoc (hash (:channel p)) game-state))
			(game/run-game game-state)
			"ok")
		"error"))

(defn user-move [ch-id y]
  (if-let [state (get @game ch-id)]
    (do
    	(game/platform-move state ch-id y)
    	"ok")
    "error"))

(defn user-game-end [ch-id]
  (if-let [user (if (= ch-id (-> (deref (get @game ch-id)) :left :user :channel hash)) 
                  (-> (deref (get @game ch-id)) :left :user)
                  (-> (deref (get @game ch-id)) :right :user))]
    (do 
      (swap! game dissoc ch-id)      
      (send-all {
        :method "add-user" 
        :name (:name user) 
        :id (hash (:channel user))})
      (swap! lobby assoc ch-id user)
      "ok")
    "error"))

(defn action-handler [channel data]
  (let [json (json/parse-string data)]
    (case (json "method")
      "action-signin" (signin (json "login") channel)
      "invate-fight" (user-start-fight (hash channel) (json "opponent"))
      "user-move" (user-move (hash channel) (json "pos"))
      "user-game-end" (user-game-end (hash channel)))))

(defn websocket-handler [request]
	(with-channel request channel
    (on-close channel 
    	(fn [status] (user-exit (hash channel))))
    (on-receive channel 
    	(fn [data] (action-handler channel data)))))

(defroutes app 
	(GET "/" [] (resource-response "index.html" {:root "public"}))
	(GET "/ws" request (websocket-handler request))
	(resources "/" {:root "public"})
	(not-found "<h1>404: Page not found</h1>"))

(defn -main [& args]
	(run-server 
		(-> #'app
			site
			wrap-reload) {:port 8080}))