(ns pong-game.core
  (:gen-class)
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
  (send-all {:method "event-rem-user" :id channel-id}))

(defn- user-exit [channel-id]
	(lobby-exit channel-id)
  (when-let [game-agent (get @game channel-id)]
    (game/stop-game game-agent))
	(swap! game dissoc channel-id))

(defn- get-user-list []
  (map #(hash-map :id (key %) :name (:name (val %))) @lobby))

(defn signin [username channel]
  (send! channel (json/generate-string 
    (if (empty? (filter #(= username (:name %)) (vals @lobby)))
      (let [user-list (get-user-list)]
        (send-all {
          :method "event-new-user" 
          :name username 
          :id (hash channel)})
        (swap! lobby assoc (hash channel) {:name username :channel channel})
        { :method "event-signin" 
          :status "ok" 
          :text "Вход выполнен спешно" 
          :user-list user-list})
      { :method "event-signin" 
        :status "fail" 
        :text "такое имя уже занято" 
        :user-list []}))))

(defn start-fight [me-id op-id]
	(when-let [[me op] (map #(get @lobby %) [me-id op-id])]
		(let [game-state (game/new-game me op)]
			(doseq [p [me op]]
        (lobby-exit (hash (:channel p)))
				(send! (:channel p) (json/generate-string {:method "event-fight"}))        
				(swap! game assoc (hash (:channel p)) game-state))
			(game/run-game game-state))))

(defn user-move [ch-id y]
  (when-let [state (get @game ch-id)]
    (game/platform-move state ch-id y)))

(defn user-game-end [ch-id]
  (let [user (if (= ch-id (-> (deref (get @game ch-id)) :left :user :channel hash)) 
                  (-> (deref (get @game ch-id)) :left :user)
                  (-> (deref (get @game ch-id)) :right :user))
        user-list (get-user-list)]    
    (swap! game dissoc ch-id)      
    (send-all {
      :method "event-new-user" 
      :name (:name user) 
      :id (hash (:channel user))})
    (swap! lobby assoc ch-id user)
    (send! (:channel user) (json/generate-string {
      :method "event-signin" 
      :status "ok" 
      :text "Вход выполнен спешно" 
      :user-list user-list}))))

(defn action-handler [channel data]
  (let [json (json/parse-string data)]
    (case (json "method")
      "action-signin" (signin (json "login") channel)
      "action-start-fight" (start-fight (hash channel) (Integer/parseInt (json "with")))
      "action-move" (user-move (hash channel) (json "pos"))
      "action-game-end" (user-game-end (hash channel)))))

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