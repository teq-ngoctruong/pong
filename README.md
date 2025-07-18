# Pong Game

This project is fully created by Copilot with agent mode (except this line :D)

A classic single-player Pong game implemented in ClojureScript using shadow-cljs. Play against an AI opponent!

## Features

- Single-player Pong game against AI
- Smooth paddle movement with keyboard controls
- Ball physics with improved collision detection
- Score tracking (Player vs Computer)
- **Progressive difficulty**: Ball speed increases every 2 paddle hits
- Rally counter and speed multiplier display
- Pause/resume functionality
- Intelligent AI opponent

## Controls

- **Player**: Up/Down arrow keys to move paddle up/down
- **Space**: Start/pause the game
- **AI**: Computer-controlled right paddle

## Getting Started

### Prerequisites

- Node.js (v14 or later)
- Java (for ClojureScript compilation)

### Installation

1. Clone or download this project
2. Install dependencies:
   ```bash
   npm install
   ```

### Development

1. Start the shadow-cljs development server:
   ```bash
   npm run dev
   ```

2. Start a local HTTP server to serve the game:
   ```bash
   python3 -m http.server 8000 --directory public
   ```

3. Open your browser and navigate to `http://localhost:8000`

### Building for Production

To create a production build:

```bash
npm run build
```

This will generate optimized JavaScript files in the `public/js` directory.

## Project Structure

```
pong-game/
├── src/
│   └── pong_game/
│       └── core.cljs          # Main game logic
├── public/
│   ├── index.html             # Game HTML page
│   └── js/                    # Generated JavaScript files
├── shadow-cljs.edn            # Shadow-cljs configuration
└── package.json               # Node.js dependencies
```

## Game Architecture

The game is built using ClojureScript with the following key components:

- **Game State**: Managed using a ClojureScript atom containing ball position, paddle positions, scores, and keyboard state
- **Game Loop**: Uses `requestAnimationFrame` for smooth 60 FPS gameplay
- **Physics**: Simple collision detection for ball-paddle and ball-wall interactions
- **Rendering**: Canvas-based rendering with HTML5 Canvas API

## How to Play

1. Press **Space** to start the game
2. Use **Up/Down arrow keys** to move your paddle up and down
3. Try to hit the ball past the AI opponent
4. Score points when the AI misses the ball
5. **Challenge**: The ball speed increases every 2 times you hit it!
6. Watch your rally count and speed multiplier in the top display
7. Press **Space** again to pause/resume at any time

Challenge yourself against the AI and see how many points you can score as the game gets faster!
