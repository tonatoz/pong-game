(defproject pong-game "0.1.5-SNAPSHOT"
  :description "Online PingPong game"
  :url "http://clojure.tonatoz.com/"
  :dependencies [[org.clojure/clojure "1.6.0"]
  							 [http-kit "2.1.16"]
  							 [compojure "1.1.8"]
  							 [ring "1.2.2"]
  							 [cheshire "5.3.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [enfocus "2.0.2"]]  
  :plugins [[lein-cljsbuild "1.0.3"]]
  :min-lein-version "2.0.0"
  :source-paths ["src/clj"]
  :main pong-game.core
  :uberjar-name "pong_server.jar"
  :aot [pong-game.core]
  :cljsbuild {
    :builds [{
        :source-path "src/cljs"
        :compiler {
          :output-to "resources/public/js/client.js" 
          :optimizations :whitespace
          :pretty-print true}}]})
