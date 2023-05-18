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
             [:urgency [:and :int [:>= 0] [:<= 10]]]]))

(defn listen! [& {app :app mqtt-client
                  :mqtt-client topic
                  :topic logger
                  :logger urgency-threshold
                  :urgency-threshold}]
  (let [note-chan (mqtt/subscribe! mqtt-client topic)]
    (go-loop [note-msg (<! note-chan)]
      (if note-msg
        (let [note (:payload note-msg)]
          (if (t/validate Notification note)
            (when (>= (:urgency note) urgency-threshold)
              (notify/send-notification! mqtt-client (assoc note :app app))
              (log/info! logger (format "ignoring low-urgency message (%s): %s"
                                        (:urgency note)
                                        (:summary note))))
            (let [err (humanize (t/explain Notification note))]
              (log/error! logger (format "rejecting invalid notification: %s (%s)\n%s"
                                         (:summary err) (:body err)
                                         (pprint-to-string note)))))
          (recur (<! note-chan)))
        (log/info! logger "stopping")))))
