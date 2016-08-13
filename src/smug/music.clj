(ns smug.music
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer :all]
            [clojure.core.logic.fd :as fd]))

;;; LOGIC

(defn pitcho [p]
  (fd/in p (fd/interval 1 7)))

(defn note-valueo [v]
  (fd/in v (fd/domain 1 2 4 8 16)))

(defne noteo [note]
  ([ [p v] ]
   (pitcho p)
   (note-valueo v)))

(defne noteso [notes]
  ([ [] ])
  ([ [n . ns] ]
   (noteo n)
   (noteso ns)))

(defne notes-total-valueo [notes total]
  ([ [] _ ]
   (fd/== total 0))
  ([ [[p v] . ns] _ ]
   (fresh (s)
     (note-valueo v)
     (fd/+ v s total)
     (notes-total-valueo ns s))))

(defn groupo [notes duration]
  (all
    (matche [notes]
            ([ [[_ v]] ]
             (fd/>= v 4)
             (fd/== v duration))
            ([ [[_ 1] [_ 1] [_ 1] [_ 1]] ]
             (fd/== duration 4))
            ([ [[_ 1] [_ 2] [_ 1]] ]
             (fd/== duration 4))
            ([ [[_ 2] [_ 1] [_ 1]] ]
             (fd/== duration 4))
            ([ [[_ 1] [_ 1] [_ 2]] ]
             (fd/== duration 4))
            ([ [[_ 2] [_ 2]] ]
             (fd/== duration 4))
            ([ [[_ 2] [_ 4] [_ 2]] ]
             (fd/== duration 8)))
    (noteso notes)
    (note-valueo duration)))

(defne groupso [groups duration]
  ([ [] _ ]
   (fd/== duration 0))
  ([ [g . gs] _ ]
   (fresh [group-total sub-total]
     (groupo g group-total)
     (fd/+ group-total sub-total duration)
     (groupso gs sub-total))))

(defne bar-noteso [bar notes]
  ([ [] _ ]
   (== notes []))
  ([ [group . groups] _ ]
   (fresh [groups-notes]
     (bar-noteso groups groups-notes)
     (appendo group groups-notes notes))))

(defne scaleo [direction notes]
  ([ _ [] ])
  ([ _ [[p _]] ] (pitcho p))
  ([ _ [[p1 _] . [p2 v2] . ns]]
   (pitcho p1)
   (pitcho p2)
   (conde
    [(== direction :asc)
     (fd/+ p1 1 p2)]
    [(== direction :desc)
     (fd/- p1 1 p2)])
   (fresh [tail]
     (conso [p2 v2] ns tail)
     (scaleo direction tail))))

(defn baro [bar]
  (fresh [dir notes]
    (groupso bar 16)
    (bar-noteso bar notes)
    (scaleo dir notes)))

;;; CONVERSION

(defn ->pitch [p]
  (nth [:c :d :e :f :g :a :b] (- p 1)))

(defn ->note-value [d]
  (/ d 16))

(defn ->note [[p d]]
  [(->pitch p)
   (->note-value d)])

(defn ->bar [bar]
  (map ->note bar))

(defn flatten-groups [groups]
  (map #(apply concat %1) groups))

;;; INTERFACE

(defn generate-score [n]
  (let [groups (run n [q]
                 (baro q))
        bars (flatten-groups groups)]
    {:bars (map ->bar bars)}))
