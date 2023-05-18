(ns tattler.core
  (:require [notifier.core :as notify]
            [milquetoast.client :as mqtt]
            [clojure.core.async :refer [go-loop <!]]
            [fudo-clojure.logging :as log]
            [clojure.pprint :refer [pprint]]
            [malli.core :as t]
            [malli.error :refer [humanize]]))

(defn- sized-string [min max]
  (t/schema [:string {:min min :max max}]))

(defn- pprint-to-string [str]
  (with-out-str (pprint str)))

(def Notification
  (t/schema [:map
             [:summary (sized-string 1 80)]
             [:body    (sized-string 1 256)]
             [:urgency {:optional true} [:enum "low" "medium" "high"]]]))

(defn listen! [& {app :app mqtt-client :mqtt-client topic :topic logger :logger}]
  (let [note-chan (mqtt/subscribe! mqtt-client topic)]
    (go-loop [note (<! note-chan)]
      (if note
        (do (if (t/validate Notification note)
              (notify/send-notification! mqtt-client
                                         {
                                          :app     app
                                          :summary (:summary note)
                                          :body    (:body note)
                                          :urgency (-> note :urgency (keyword))
                                          })
              (let [err (humanize (t/explain Notification note))]
                (log/error! logger (format "rejecting invalid notification: %s (%s)\n%s"
                                           (:summary err) (:body err)
                                           (pprint-to-string note)))))
            (recur (<! note-chan)))
        (log/info! logger "stopping")))))
