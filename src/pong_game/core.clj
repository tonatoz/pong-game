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
;; item -> {user-channel (ref: user-game)}
(def game (atom {})) 

(defn send-all [data]
	(doseq [ch (map :channel (vals @lobby))]
		(send! ch (json/generate-string data))))

(defn user-exit [channel-id]
	(swap! lobby dissoc channel-id)
	(if-let [game-agent (get @game channel-id)]
		(send game-agent assoc-in :status :stop))
	(swap! game dissoc channel-id)
	(send-all {:method "rem-user" :id channel-id}))

(defn user-signin [username channel]
	(if (empty? (filter #(= username (:name %)) @lobby))
		(do 
			(send-all {
				:method "add-user" 
				:name username 
				:id (hash channel)})
			(swap! lobby assoc (hash channel) {:name username :channel channel})
			"ok")
		"error"))

(defn user-start-fight [me-id op-id]
	(if-let [[me op] (map #(get @lobby %) [me-id op-id])]
		(let [game-state (game/new-game me op)]
			(doseq [p [me op]]
				(send! (:channel p) (json/generate-string {:method "fight"}))
				(user-exit (hash (:channel p)))
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

(defn action-handler [channel data]
  (let [json (json/parse-string data)]
    (json/generate-string
      {:method (json "method")
       :result (case (json "method")
          "signin" (user-signin (json "login") channel)
          "invate-fight" (user-start-fight (hash channel) (json "opponent"))
          "user-move" (user-move (hash channel) (json "pos")))})))

(defn websocket-handler [request]
	(with-channel request channel
    (on-close channel 
    	(fn [status] (user-exit (hash channel))))
    (on-receive channel 
    	(fn [data] (send! channel (action-handler channel data))))))

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