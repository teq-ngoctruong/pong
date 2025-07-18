(ns pong-game.core)

;; Game state
(def game-state
  (atom
   {:canvas nil
    :ctx nil
    :game-running false
    :ball {:x 400 :y 200 :dx 5 :dy 3 :radius 10}
    :paddle1 {:x 10 :y 150 :width 10 :height 100 :speed 8}
    :paddle2 {:x 780 :y 150 :width 10 :height 100 :speed 8}
    :score {:player1 0 :player2 0}
    :keys {:up false :down false :space false}
    :collision-count 0
    :speed-multiplier 1.0}))

;; Constants
(def CANVAS-WIDTH 800)
(def CANVAS-HEIGHT 400)
(def PADDLE-SPEED 8)
(def BALL-SPEED 5)

;; Utility functions
(defn clamp [value min-val max-val]
  (max min-val (min max-val value)))

(defn reset-ball! []
  (swap! game-state assoc-in [:ball :x] (/ CANVAS-WIDTH 2))
  (swap! game-state assoc-in [:ball :y] (/ CANVAS-HEIGHT 2))
  (swap! game-state assoc-in [:ball :dx] (* (if (> (rand) 0.5) 1 -1) BALL-SPEED))
  (swap! game-state assoc-in [:ball :dy] (* (if (> (rand) 0.5) 1 -1) (+ 2 (rand-int 3))))
  ;; Reset collision count and speed multiplier when ball resets
  (swap! game-state assoc :collision-count 0)
  (swap! game-state assoc :speed-multiplier 1.0)
  ;; Update UI
  (set! (.-innerHTML (.getElementById js/document "speed-multiplier")) "1.0")
  (set! (.-innerHTML (.getElementById js/document "collision-count")) "0"))

(defn increase-speed-if-needed! []
  (let [collision-count (:collision-count @game-state)]
    (when (and (> collision-count 0) (zero? (mod collision-count 2)))
      ;; Increase speed by 10% every 2 collisions
      (swap! game-state update :speed-multiplier #(* % 1.1))
      (let [multiplier (:speed-multiplier @game-state)]
        ;; Apply speed multiplier to ball
        (swap! game-state update-in [:ball :dx] #(* (/ % (/ multiplier 1.1)) multiplier))
        (swap! game-state update-in [:ball :dy] #(* (/ % (/ multiplier 1.1)) multiplier))
        ;; Update UI
        (set! (.-innerHTML (.getElementById js/document "speed-multiplier")) 
              (.toFixed multiplier 1))))))

(defn update-ui! []
  (let [collision-count (:collision-count @game-state)]
    (set! (.-innerHTML (.getElementById js/document "collision-count")) collision-count)))

(defn update-score! [player]
  (swap! game-state update-in [:score player] inc)
  (let [score (get-in @game-state [:score player])]
    (when (= player :player1)
      (set! (.-innerHTML (.getElementById js/document "player1-score")) score))
    (when (= player :player2)
      (set! (.-innerHTML (.getElementById js/document "player2-score")) score)))
  (reset-ball!))

;; Game logic
(defn update-ai-paddle! []
  (let [{:keys [ball paddle2]} @game-state
        ball-y (:y ball)
        paddle-center-y (+ (:y paddle2) (/ (:height paddle2) 2))
        ai-speed 6] ; Slightly slower than player for fairness
    
    ;; Simple AI: move paddle toward ball with some reaction delay
    (cond
      (> ball-y (+ paddle-center-y 10)) ; Ball is below paddle center
      (swap! game-state update-in [:paddle2 :y] 
             #(clamp (+ % ai-speed) 0 (- CANVAS-HEIGHT (:height paddle2))))
      
      (< ball-y (- paddle-center-y 10)) ; Ball is above paddle center
      (swap! game-state update-in [:paddle2 :y] 
             #(clamp (- % ai-speed) 0 (- CANVAS-HEIGHT (:height paddle2)))))))

(defn update-paddles! []
  (let [{:keys [paddle1 keys]} @game-state]
    ;; Player 1 (Up/Down arrow keys)
    (when (:up keys)
      (swap! game-state update-in [:paddle1 :y] #(clamp (- % PADDLE-SPEED) 0 (- CANVAS-HEIGHT (:height paddle1)))))
    (when (:down keys)
      (swap! game-state update-in [:paddle1 :y] #(clamp (+ % PADDLE-SPEED) 0 (- CANVAS-HEIGHT (:height paddle1)))))
    
    ;; AI Player 2
    (update-ai-paddle!)))

(defn ball-paddle-collision? [ball paddle]
  (let [{ball-x :x ball-y :y :keys [radius]} ball
        {paddle-x :x paddle-y :y :keys [width height]} paddle]
    (and (< (- ball-x radius) (+ paddle-x width))
         (> (+ ball-x radius) paddle-x)
         (< (- ball-y radius) (+ paddle-y height))
         (> (+ ball-y radius) paddle-y))))

(defn update-ball! []
  (let [{:keys [ball paddle1 paddle2]} @game-state
        {:keys [x y dx dy radius]} ball
        new-x (+ x dx)
        new-y (+ y dy)]
    
    ;; Wall collision (top and bottom) - check before updating position
    (let [final-dy (if (or (<= new-y radius) (>= new-y (- CANVAS-HEIGHT radius)))
                     (- dy)
                     dy)]
      (swap! game-state assoc-in [:ball :dy] final-dy))
    
    ;; Paddle collision - check before updating position
    (let [test-ball {:x new-x :y new-y :radius radius}
          final-dx (cond
                     (ball-paddle-collision? test-ball paddle1)
                     (do
                       ;; Increment collision count when ball hits player paddle
                       (swap! game-state update :collision-count inc)
                       (update-ui!)
                       (increase-speed-if-needed!)
                       (swap! game-state update-in [:ball :dy] #(+ % (* (- (rand) 0.5) 2)))
                       (abs dx)) ; Ensure ball moves right after hitting left paddle
                     
                     (ball-paddle-collision? test-ball paddle2)
                     (do
                       (swap! game-state update-in [:ball :dy] #(+ % (* (- (rand) 0.5) 2)))
                       (- (abs dx))) ; Ensure ball moves left after hitting right paddle
                     
                     :else dx)]
      (swap! game-state assoc-in [:ball :dx] final-dx))
    
    ;; Update ball position after collision checks
    (swap! game-state update-in [:ball :x] + (get-in @game-state [:ball :dx]))
    (swap! game-state update-in [:ball :y] + (get-in @game-state [:ball :dy]))
    
    ;; Score when ball goes off screen
    (let [final-x (get-in @game-state [:ball :x])]
      (when (< final-x 0)
        (update-score! :player2))
      (when (> final-x CANVAS-WIDTH)
        (update-score! :player1)))))

;; Rendering
(defn clear-canvas! [ctx]
  (set! (.-fillStyle ctx) "#000")
  (.fillRect ctx 0 0 CANVAS-WIDTH CANVAS-HEIGHT))

(defn draw-paddle! [ctx paddle]
  (set! (.-fillStyle ctx) "#fff")
  (.fillRect ctx (:x paddle) (:y paddle) (:width paddle) (:height paddle)))

(defn draw-ball! [ctx ball]
  (set! (.-fillStyle ctx) "#fff")
  (.beginPath ctx)
  (.arc ctx (:x ball) (:y ball) (:radius ball) 0 (* 2 (.-PI js/Math)))
  (.fill ctx))

(defn draw-center-line! [ctx]
  (set! (.-strokeStyle ctx) "#fff")
  (set! (.-lineWidth ctx) 2)
  (.setLineDash ctx #js [5 5])
  (.beginPath ctx)
  (.moveTo ctx (/ CANVAS-WIDTH 2) 0)
  (.lineTo ctx (/ CANVAS-WIDTH 2) CANVAS-HEIGHT)
  (.stroke ctx))

(defn render! []
  (let [{:keys [ctx ball paddle1 paddle2]} @game-state]
    (when ctx
      (clear-canvas! ctx)
      (draw-center-line! ctx)
      (draw-paddle! ctx paddle1)
      (draw-paddle! ctx paddle2)
      (draw-ball! ctx ball))))

;; Game loop
(defn game-loop! []
  (when (:game-running @game-state)
    (update-paddles!)
    (update-ball!)
    (render!)
    (js/requestAnimationFrame game-loop!)))

;; Event handling
(defn handle-keydown! [e]
  (let [key (.-key e)]
    (case key
      "ArrowUp" (do (.preventDefault e) (swap! game-state assoc-in [:keys :up] true))
      "ArrowDown" (do (.preventDefault e) (swap! game-state assoc-in [:keys :down] true))
      " " (do (.preventDefault e) 
              (swap! game-state update :game-running not)
              (when (:game-running @game-state) (game-loop!)))
      nil)))

(defn handle-keyup! [e]
  (let [key (.-key e)]
    (case key
      "ArrowUp" (swap! game-state assoc-in [:keys :up] false)
      "ArrowDown" (swap! game-state assoc-in [:keys :down] false)
      nil)))

;; Initialization
(defn init-game! []
  (let [canvas (.getElementById js/document "game-canvas")
        ctx (.getContext canvas "2d")]
    (swap! game-state assoc :canvas canvas :ctx ctx)
    (reset-ball!)
    (render!)
    
    ;; Add event listeners
    (.addEventListener js/document "keydown" handle-keydown!)
    (.addEventListener js/document "keyup" handle-keyup!)))

(defn init []
  (js/console.log "Pong game initialized!")
  (js/setTimeout init-game! 100)) ; Small delay to ensure DOM is ready
