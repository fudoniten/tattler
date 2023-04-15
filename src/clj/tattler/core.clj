(ns tattler.core
  (:import [org.freedesktop.dbus DBusConnection]
           [org.freedesktop Notifications])
  (:gen-class))

(def ^:private ^:const NOTIFICATIONS_PATH "/org/freedesktop/Notifications")
(def ^:private ^:const NOTIFICATIONS_BUS "org.freedesktop.Notifications")

(defn get-server-capabilities []
  (let [dbus (DBusConnection/getConnection DBusConnection/SESSION)]
    (.getRemoteObject dbus NOTIFICATIONS_BUS NOTIFICATIONS_PATH (.class Notifications))))

#_(defn send [{app :app conn :conn} notification]
  (let []))

(defn -main [& args]
  (println (get-server-capabilities)))
