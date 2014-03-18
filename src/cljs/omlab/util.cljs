(ns omlab.util
  (:require [cljs.reader :as reader]
            [clojure.string :as string])
  (:import [goog.ui IdGenerator]
           [goog.i18n DateTimeParse]
           [goog.i18n DateTimeFormat]
           [goog.i18n.DateTimeFormat Format]
           [goog.crypt Md5]))

(defn now []
  (js/Date.))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn with-guid [m]
  (assoc m :guid (guid)))

(defn md5-hash [text]
  (->> (doto (Md5.)
        (.update text))
       (.digest)
       goog.crypt.byteArrayToHex))

(defn email-digest [email]
  (->> email
       (.trim)
       (.toLowerCase)
       (md5-hash)))

(defn convert-sid [m]
  (if (instance? UUID (:sid m))
    (assoc m :sid (.-uuid (:sid m)))
    m))

(defn has-keys? [m & ks]
  (every? true? (map (partial contains? m) ks)))

(defn has-blank-strings? [m & ks]
  (let [tm (select-keys m ks)]
    (if (apply has-keys? tm ks)
      (->> tm
           (vals)
           (map str)
           (map string/trim)
           (remove (complement empty?))
           (seq))
      :keys-missing)))

(defn update-ids [m]
  (-> m convert-sid with-guid))

(defn pad [length char]
  (.join (js/Array (inc length)) char))

(defn padl [n length char]
  (let [s (str n)
        slen (.-length s)]
    (if (< slen length)
      (apply str (pad (- length slen) char) s)
      s)))

(defn padr [n length char]
  (let [s (str n)
        slen (.-length s)]
    (if (< slen length)
      (apply str s (pad (- length slen) char))
      s)))

(defn parse-iso-date [text]
  (let [dt (js/Date. 0 0)]
    (when (= 10 (.strictParse (DateTimeParse. "yyyy-MM-dd") text dt))
      dt)))

(defn format-date [pattern date]
  (when date (.format (DateTimeFormat. pattern) date)))

(defn format-short-time [time]
  (when time (.format (DateTimeFormat. Format.SHORT_TIME) time)))

(defn format-long-time [time]
  (when time (.format (DateTimeFormat. Format.LONG_TIME) time)))

(defn format-short-date [date]
  (when date (.format (DateTimeFormat. Format.SHORT_DATE) date)))

(defn format-long-date [date]
  (when date (.format (DateTimeFormat. Format.LONG_DATE) date)))

(defn format-iso-date [date]
  (when date
    (let [year (padl (.getFullYear date) 4 "0")
          month (padl (inc (.getMonth date)) 2 "0")
          date (padl (.getDate date) 2 "0")]
      (str year "-" month "-" date))))

(defn format-short-datetime [date]
  (when date (.format (DateTimeFormat. Format.SHORT_DATETIME) date)))

(defn format-long-datetime [date]
  (when date (.format (DateTimeFormat. Format.LONG_DATETIME) date)))

(defn format-full-datetime [date]
  (when date (.format (DateTimeFormat. Format.FULL_DATETIME) date)))
