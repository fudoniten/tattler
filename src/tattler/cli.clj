(ns tattler.cli
  (:require [clojure.core.async :as async :refer [>!! <!!]]
            [clojure.tools.cli :as cli]
            [clojure.set :as set]
            [clojure.string :as str]
            [tattler.core :as tattler]
            [milquetoast.client :as mqtt]
            [fudo-clojure.logging :as log])
  (:gen-class))

(def cli-opts
  [["-v" "--verbose" "Provide verbose output."]

   [nil "--app-name APP" "Name to report for this application."]

   [nil "--mqtt-host HOSTNAME" "Hostname of MQTT server."]
   [nil "--mqtt-port PORT" "Port on which to connect to the MQTT server."
    :parse-fn #(Integer/parseInt %)]
   [nil "--mqtt-user USER" "User as which to connect to MQTT server."]
   [nil "--mqtt-password-file PASSWD_FILE" "File containing password for MQTT user."]

   [nil "--notification-topic TOPIC" "MQTT topic to which events should be published."]])

(defn- msg-quit [status msg]
  (println msg)
  (System/exit status))

(defn- usage
  ([summary] (usage summary []))
  ([summary errors] (->> (concat errors
                                 ["usage: tattler-client [opts]"
                                  ""
                                  "Options:"
                                  summary])
                         (str/join \newline))))

(defn- parse-opts [args required cli-opts]
  (let [{:keys [options] :as result} (cli/parse-opts args cli-opts)
        missing (set/difference required (-> options (keys) (set)))
        missing-errors (map #(format "missing required parameter: %s" (name %))
                            missing)]
    (update result :errors concat missing-errors)))

(defn -main [& args]
  (let [required-args #{:mqtt-host :mqtt-port :mqtt-user :mqtt-password-file :app-name :notification-topic}
        {:keys [options _ errors summary]} (parse-opts args required-args cli-opts)]
    (when (seq errors) (msg-quit 1 (usage summary errors)))
    (let [{:keys [mqtt-host mqtt-port mqtt-user mqtt-password-file app-name notification-topic]} options
          catch-shutdown (async/chan)
          mqtt-client (mqtt/connect-json! :host mqtt-host
                                          :port mqtt-port
                                          :username mqtt-user
                                          :password (-> mqtt-password-file
                                                        (slurp)
                                                        (str/trim)))
          logger (log/print-logger)]
      (tattler/listen! :app         app-name
                       :mqtt-client mqtt-client
                       :topic       notification-topic
                       :logger      logger)
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn [] (>!! catch-shutdown true))))
      (<!! catch-shutdown)
      ;; Stopping the MQTT will stop tattler
      (mqtt/stop! mqtt-client)
      (System/exit 0))))
