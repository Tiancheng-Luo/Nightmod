(ns nightmod.public
  (:require [clojail.core :as jail]
            [clojure.java.io :as io]
            [nightmod.core :refer :all]
            [play-clj.core :refer :all]))

(defn set-game-screen!
  [& screens]
  (->> (apply set-screen! nightmod (conj (vec screens) overlay-screen))
       (fn [])
       (app! :post-runnable)))

(defmacro load-game-file
  [n]
  (some->> (or (io/resource n)
               (throw (Throwable. (str "File not found:" n))))
           slurp
           (format "(do %s\n)")
           jail/safe-read))
