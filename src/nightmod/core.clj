(ns nightmod.core
  (:require [cemerick.pomegranate :as pom]
            [clojure.java.io :as io]
            [nightmod.git :as git]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [nightmod.sandbox :as sandbox]
            [nightcode.editors :as editors]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightcode.window :as window]
            [seesaw.core :as s]
            [seesaw.util :as s-util])
  (:import [java.awt BorderLayout Canvas Dimension Window]
           [java.awt.event ComponentAdapter WindowAdapter]
           [javax.swing JLayeredPane]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

; allow s/select to work with Canvas
(extend-protocol s-util/Children
  java.awt.Component (children [this] nil))

(defn create-layered-pane
  "Returns the layered pane holding the editor pane."
  []
  (let [pane (editors/create-pane)]
    (doto (JLayeredPane.)
      (.setPreferredSize (Dimension. u/editor-width u/window-height))
      (.addComponentListener (proxy [ComponentAdapter] []
                               (componentResized [e]
                                 (->> (.getComponent e)
                                      .getHeight
                                      (.setBounds pane 0 0 u/editor-width)))))
      (.add pane))))

(defn protect-file!
  "Prevents renaming or deleting a file."
  [path]
  (intern 'nightcode.file-browser
          'protect-file?
          #(= % path)))

(defn load-game!
  "Loads game into the canvas and runs it in a sandbox."
  [path]
  (pom/add-classpath path)
  (let [core (.getCanonicalPath (io/file path "core.clj"))]
    (protect-file! core)
    (sandbox/run-file! core)))

(defn create-window
  "Creates the main window."
  [canvas]
  (doto (s/frame :title (str "Nightmod " (or (some-> "nightmod.core"
                                                     nc-utils/get-project
                                                     (nth 2))
                                             "beta"))
                 :width u/window-width
                 :height u/window-height
                 :on-close :nothing)
    ; add canvas and editor pane
    (-> .getContentPane (doto
                          (.add (s/border-panel :center canvas))))
    (-> .getGlassPane (doto
                        (.setLayout (BorderLayout.))
                        (.add (create-layered-pane) BorderLayout/EAST)
                        (.setVisible false)))
    ; listen for keys while modifier is down
    (shortcuts/listen-for-shortcuts!
      (fn [key-code]
        (case key-code
          ; page up
          33 (editors/move-tab-selection! -1)
          ; page down
          34 (editors/move-tab-selection! 1)
          ; Q
          81 (window/confirm-exit-app!)
          ; W
          87 (editors/close-selected-editor!)
          ; else
          false)))
    ; set various window properties
    window/enable-full-screen!
    window/add-listener!))

(defn -main
  "Launches the main window."
  [& args]
  (sandbox/set-policy!)
  (window/set-theme! args)
  (add-watch u/project-dir
             :load-game
             (fn [_ _ _ path]
               (load-game! path)))
  (s/invoke-now
    (let [canvas (Canvas.)]
      (s/show! (reset! ui/root (create-window canvas)))
      (LwjglApplication. screens/nightmod canvas)))
  (Keyboard/enableRepeatEvents true))
