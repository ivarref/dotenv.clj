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

(defn env
  ([]
   (base-env))
  ([k]
   (env k nil))
  ([k default-value]
   (get (base-env) (name k) default-value)))