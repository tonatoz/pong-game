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
;; item -> {channel-id {:game game-agent :play (fn)}}
(def game (atom {})) 

(defn- uuid [] 
	(str (java.util.UUID/randomUUID)))

(defn send-all [data]
	(doseq [ch (map :channel (vals @lobby))]
		(send! ch (json/generate-string data))))

(defn user-exit [channel-id]
	(swap! lobby dissoc channel-id)
	(swap! game dissoc channel-id)
	(send-all {:method "rem-user" :id channel-id}))

(defn user-signin [username channel]
	(if-not (contains? @lobby username)
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
		(let [game-id (uuid)]
			(send! (:channel op) (json/generate-string {:method "fight" :id game-id}))
			(user-exit me-id)
			(user-exit op-id)
			(swap! game assoc game-id (game/new-game me op))
			"ok")
		"error"))

(defn user-move [game-id ch-id y]
  (if-let [state (get @game game-id)]
    ()))

(defn action-handler [channel data]
  (let [json (json/parse-string data)]
    (json/generate-string
      {:method (json "method")
       :result (case (json "method")
          "signin" (user-signin (json "login") channel)
          "invate-fight" (user-start-fight (hash channel) (json "opponent"))
          "user-move" (user-move (json "id") (hash channel) (json "pos")))})))

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