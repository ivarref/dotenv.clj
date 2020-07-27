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

(defn map-add-ns
  [m new-ns]
  (reduce-kv (fn [o k v]
               (assoc o (keyword (some-> new-ns name) k) v))
             (sorted-map)
             m))

(defn env
  "Returns a map from System/getenv, System/getProperties, the file `.env`
  and `*override-env*`. Keeps only keys starting with `service`.
  Removes `service` prefix and trailing underscore and dashes from keys.
  Keys are keywordized and an `add-ns` is optionally added as namespace.

  Example:
  (binding [*override-env* {\"MY_APP_VAR1\"  \"123\"
                            \"MY_APP__VAR2\" \"999\"}]
    (env {:service \"MY_APP\"}))

  Will return:
  {:VAR1 \"123\" :VAR2 \"999\"}
  "
  [{:keys [service add-ns]
    :or   {service ""
           add-ns  nil}}]
  (as-> (base-env) m
        (mapcat (fn [[k v]]
                  (when (str/starts-with? k service)
                    (let [k (subs k (count service))]
                      [[(str/replace k #"(_|-)*" "") v]])))
                m)
        (into {} m)
        (map-add-ns m add-ns)))

(comment
  (str/replace "__--A" #"(_|-)*" ""))

(comment
  (do
    (require '[clojure.test :as test])
    (test/is
      (= (binding [*override-env* {"MY_APP_VAR1"  "123"
                                   "MY_APP__VAR2" "999"
                                   "MY_APPVAR3"   "777"}]
           (env {:service "MY_APP"}))
         {:VAR1 "123", :VAR2 "999", :VAR3 "777"}))
    (test/is
      (= (binding [*override-env* {"MY_APP_VAR1"  "123"
                                   "MY_APP__VAR2" "999"
                                   "MY_APPVAR3"   777}]
           (env {:service "MY_APP"
                 :add-ns  :env}))
         #:env{:VAR1 "123", :VAR2 "999", :VAR3 777}))))
