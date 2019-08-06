(ns dotenv
   (:require [clojure.string :as str]
             [clojure.java.io :as io]))

(defn- to-pairs [rawstring]
  "converts a string containing the contents of a .env file into a list of pairs"
  (let [lines (str/split-lines rawstring)]
    (->> lines
         (filter #(str/includes? % "=")) ; keep only lines containing '='
         (remove #(str/starts-with? % "#")) ; remove lines starting with # (comments)
         (map #(str/split % #"=" 2)))))

(defn- load-env-file [filename]
  "loads an env file into a map"
  (when (.exists (io/as-file filename))
    (->> filename
         slurp
         to-pairs
         (into {}))))

(def ^:dynamic *override-env* {})

(defn base-env []
  (into {} [(System/getenv)
            (System/getProperties)
            (load-env-file ".env")
            *override-env*]))

(defn lookup [e k default-value]
  (get e (name k) default-value))

(defn dev-env
  ([k]
   (dev-env k nil))
  ([k default-value]
   (lookup (base-env) k default-value)))

(def static-env
  (let [e (base-env)]
    (fn
      ([k] (lookup e k nil))
      ([k default-value] (lookup e k default-value)))))

(def env
  (if (.exists (io/file ".git"))
    dev-env
    static-env))