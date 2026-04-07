# Asterisk: Web Edition

Asterisk is a high-precision, side-scrolling survival game built for the web. Inspired by classic "helicopter" and "flappy" mechanics, it challenges players to navigate a narrow trail through a field of obstacles using a single-key control scheme.

---

## 🛠 Features

* **Physics-Based Movement:** Vertical momentum controlled entirely by the `ENTER` key.
* **Dynamic Difficulty:** Procedural obstacle generation scales obstacle density as you progress through levels.
* **Rainbow Level Colors:** Each level cycles through the rainbow palette. The player trail and obstacles change color per level using Red, Orange, Yellow, Green, Blue, Indigo, and Violet.
* **Speed Streak System:** Survival increases speed over time with additional boosts for longer streaks.
* **Persistent Scoring:** High scores are saved in the browser's LocalStorage.

---

## 🎮 How to Play

### Objective
Navigate your point (the asterisk) from the left side of the screen to the **Goal Gap** on the far right without hitting obstacles or the canvas boundaries.

### Controls
| Action | Input |
| :--- | :--- |
| **Rise** | Hold `ENTER` |
| **Fall** | Release `ENTER` |

### Rules
1.  **Don't Touch the White:** The white dots are obstacles. A single pixel of contact results in a Game Over.
2.  **Stay Inside:** Hitting the top or bottom boundary will destroy your trail.
3.  **The Goal:** Reach the invisible threshold at the far right of the canvas to trigger **Victory** and advance to the next level.

---

## 🏗 Technical Overview

The game is rendered on a single HTML5 `<canvas>` element using the 2D rendering context.

### Collision Detection
The game uses **Pixel-Perfect Collision Detection**. Instead of calculating geometric overlaps, the engine samples the pixel data directly in front of the player's coordinates:

```javascript
const pixel = ctx.getImageData(xPos + 1, yPos, 1, 1).data;
if (pixel[0] > 150 && pixel[1] > 150 && pixel[2] > 150) {
    // Collision Logic
}
```

> Note: The game samples `xPos + 1` to ensure collisions are caught reliably even at higher speeds.

### Game State Management

* `initLevel()`: Resets positions, updates level state, selects the next rainbow color, and generates obstacles.
* `startCountdown()`: A non-blocking timer that displays a ready countdown before gameplay begins.
* `gameLoop()`: Uses `requestAnimationFrame` for smooth animation and updates position, speed, and collision checks.

---

## 🚀 Getting Started

1. Clone the repository.
2. Open `asterisk.html` in any modern web browser.
3. No external dependencies or local servers are required.

---

## 👥 Contribution & Development

This project was developed collaboratively. Each major feature (collision engine, level scaling, speed streaks, and rainbow level colors) was reviewed by the team to ensure code quality and clarity.

> Team Note: This documentation was initially scaffolded via AI and then validated and refined by the development team for technical accuracy.