(ns dotenv
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- to-pairs [rawstring]
  "converts a string containing the contents of a .env file into a list of pairs"
  (let [lines (str/split-lines rawstring)]
    (->> lines
         (filter #(str/includes? % "="))                    ; keep only lines containing '='
         (remove #(str/starts-with? % "#"))                 ; remove lines starting with # (comments)
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
  (into (sorted-map) [(System/getenv)
                      (System/getProperties)
                      (load-env-file ".env")
                      *override-env*]))

(defn map-cut-key-prefix
  [m prefix]
  (reduce-kv (fn [o k v]
               (assoc o (if (str/starts-with? k prefix)
                          (subs k (count prefix))
                          k)
                        v))
             (sorted-map)
             m))

(defn map-add-ns
  [m new-ns]
  (reduce-kv (fn [o k v] (assoc o (keyword (name new-ns) k) v))
             (sorted-map)
             m))
(defn env
  [{:keys [keep-keys cut-prefix add-ns]
    :or   {keep-keys  #{}
           add-ns     nil
           cut-prefix ""}}]
  (-> (base-env)
      (map-cut-key-prefix cut-prefix)
      (select-keys (map name keep-keys))
      (map-add-ns add-ns)))

(comment
  (binding [*override-env* {"MY_APP_VAR1" "123"}]
    (env {:cut-prefix "MY_APP_"
          :add-ns     :env
          :keep-keys  #{:VAR1}})))