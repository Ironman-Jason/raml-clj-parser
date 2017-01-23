(ns raml-clj-parser.validator
  (:require [clojure.set :as set]
            [clojure.data :as data]
            [clojure.java.io :as io]
            [schema.core :as s]))

(defn- is-url?  [i]
  (try
    (when (io/as-url i) true)
    (catch java.net.MalformedURLException e false)))

(def uri (s/pred is-url?  "invalid uri format"))

(defn- versioning-base-uri? [raml]
  (when-let [base_uri (:baseUri raml)]
    (not (nil? (re-find  #"\{version\}" base_uri)))))

(defn valid-protocol?[c]
  (when (and (coll? c) (not (empty? c)))
    (every?  (fn[v] (some #(= % v) ["HTTP" "HTTPS"]))  c)))

(def protocols (s/pred valid-protocol?  "protocol only support http and/or https"))


;;For the sake of readability keep it duplicate
(def optional_version_tag
  {(s/required-key :title)         s/Str
   (s/required-key :baseUri)       uri

   (s/optional-key :version)       s/Str
   (s/optional-key :protocols)     protocols
   (s/optional-key :schemas)       s/Str
   (s/optional-key :documentation) s/Str})

(def mandatory_version_tag
  {(s/required-key :title)         s/Str
   (s/required-key :baseUri)       uri
   (s/required-key :version)       s/Str

   (s/optional-key :protocols)     protocols
   (s/optional-key :schemas)       s/Str
   (s/optional-key :documentation) s/Str})

(def root
  (s/conditional versioning-base-uri?
                 mandatory_version_tag
                 :else
                 optional_version_tag))

(defn- validate-url-parameter [raml]
  (if (versioning-base-uri? raml)
    (when-not (contains? raml :version) {:error {:version "you specified version in baseUri, version tag is needed"}})))

(defn- is-valid-root-elements?
  "https://github.com/raml-org/raml-spec/blob/master/versions/raml-08/raml-08.md#root-section"
  [raml]
  (try
    (s/validate root raml)
    (catch Exception e (.getData e))))

(defn is-valid? [raml])
