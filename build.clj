(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]))

(def lib 'org.fudo/tattler)
(def default-version "DEV")
(defn- class-dir [{:keys [target]}] (format "%s/classes" target))
(def basis (b/create-basis {:project "deps.edn"}))
(defn- jar-file [{:keys [target version]
                  :or   {version default-version}}]
  (format "%s/%s-%s.jar" target (name lib) version))

(defn- uberjar-file [{:keys [target version]
                      :or   {version default-version}}]
  (format "%s/%s-uber-%s.jar" target (name lib) version))

(def default-params
  {
   :verbose false
   :version "DEV"
   })

(defn clean [{:keys [target] :as params}]
  (b/delete {:path target})
  params)

(defn compile-java [{:keys [verbose java-src] :as params}]
  (when verbose (println (format "compiling java files in %s..." java-src)))
  (b/javac {:src-dirs   [java-src]
            :class-dir  (class-dir params)
            :basis      basis
            :javac-opts ["-source" "12" "-target" "12"]})
  params)

(defn compile-clj [{:keys [verbose clj-src] :as params}]
  (when verbose (println (format "compiling clj files in %s..." clj-src)))
  (b/compile-clj {:basis     basis
                  :src-dirs  [clj-src]
                  :class-dir (class-dir params)}))

(defn- pthru [o] (pprint o) o)

(defn- read-metadata [filename]
  (-> filename
      (slurp)
      (edn/read-string)))

(defn jar [base-params]
  (let [params (-> (merge default-params
                          (read-metadata (or (:metadata base-params)
                                             "metadata.edn"))
                          base-params)
                   (update :target str)
                   (update :version str)
                   (update :java-src str)
                   (update :clj-src str)
                   (update :main-ns str))
        {:keys [java-src clj-src main-ns version verbose]} params
        classes (class-dir params)]
    (when verbose
      (print "parameters: ")
      (pprint params))
    (compile-java params)
    (compile-clj params)
    (when verbose (println (format "writing POM file to %s..." classes)))
    (b/write-pom {
                  :class-dir classes
                  :lib       lib
                  :version   (str version)
                  :basis     basis
                  :src-dirs  [java-src clj-src]
                  })
    (when verbose (println (format "copying source files from %s to %s..."
                                   [java-src clj-src] classes)))
    (b/copy-dir {:src-dirs [java-src clj-src]
                 :target-dir classes})
    (let [jar (jar-file params)]
      (when verbose (println (format "writing JAR file to %s..." jar)))
      (b/jar {:class-dir classes
              :jar-file  jar
              :main      main-ns}))
    (when verbose (println "done!"))
    params))

(defn uberjar [base-params]
  (let [params (-> (merge default-params
                          (read-metadata (or (:metadata base-params)
                                             "metadata.edn"))
                          base-params)
                   (update :target str)
                   (update :version str)
                   (update :java-src str)
                   (update :clj-src str)
                   (update :main-ns str))
        {:keys [java-src clj-src main-ns version verbose]} params
        classes (class-dir params)]
    (when verbose
      (print "parameters: ")
      (pprint params))
    (compile-java params)
    (compile-clj params)
    (when verbose (println (format "writing POM file to %s..." classes)))
    (b/write-pom {
                  :class-dir classes
                  :lib       lib
                  :version   (str version)
                  :basis     basis
                  :src-dirs  [java-src clj-src]
                  })
    (when verbose (println (format "copying source files from %s to %s..."
                                   [java-src clj-src] classes)))
    (b/copy-dir {:src-dirs [java-src clj-src]
                 :target-dir classes})
    (let [uberjar (uberjar-file params)]
      (when verbose (println (format "writing uberjar file to %s..." uberjar)))
      (b/uber {:class-dir  classes
               :uber-file  uberjar
               :basis      basis
               :main       main-ns}))
    (when verbose (println "done!"))
    params))
