(ns tattler.core
  (:import [org.freedesktop Notifications]
           [org.freedesktop.dbus.connections.impl
            DBusConnectionBuilder]
           org.freedesktop.dbus.types.UInt32)
  (:gen-class))

(def ^:private ^:const NOTIFICATIONS_PATH "/org/freedesktop/Notifications")
(def ^:private ^:const NOTIFICATIONS_BUS "org.freedesktop.Notifications")

(defn connect-session-bus []
  (-> (DBusConnectionBuilder/forSessionBus)
      (.build)
      (.getRemoteObject NOTIFICATIONS_BUS NOTIFICATIONS_PATH Notifications)))

(defn send-notification
  [bus
   {:keys [app replace-id icon summary body actions timeout urgency]
    :or   {replace-id 0
           icon       ""
           actions    []
           timeout    -1}
    :as    args}]
  (doseq [arg [:app :summary :body]]
    (when (not (contains? args arg))
      (throw (ex-info (format "Missing required argument: %s" arg)
                      {:arg arg}))))
  (.Notify bus
           app
           (UInt32. replace-id)
           icon
           summary
           body
           actions
           hints
           timeout))

#_(defn send [{app :app conn :conn} notification]
  (let []))

(defn -main [& args]
  (send-notification (connect-session-bus)
                     {:app "my_app" :summary "Hey there" :body "How's it going now?"}))
