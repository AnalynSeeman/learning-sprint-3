# Asterisk: Web Edition

Asterisk is a high-precision, side-scrolling survival game built for the web. Inspired by classic "helicopter" and "flappy" mechanics, it challenges players to navigate a narrow trail through a field of obstacles using a single-key control scheme.

---

## 🛠 Features

* **Physics-Based Movement:** Vertical momentum controlled entirely by the `ENTER` key.
* **Dynamic Difficulty:** Procedural obstacle generation that scales in density as you progress through levels.
* **Speed Streak System:** Reward for survival! Maintaining your flight increases your speed and changes your trail color:
    * **Cyan:** Base Speed.
    * **Yellow:** Level 1 Boost (150+ frames).
    * **Orangered:** Level 2 Boost (300+ frames).
* **Persistent Scoring:** High scores are saved to your browser's LocalStorage.

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

The game is rendered on a single HTML5 `<canvas>` element using a 2D rendering context. 

### Collision Detection
The game utilizes **Pixel-Perfect Collision Detection**. Instead of calculating geometric overlaps, the engine samples the pixel data directly in front of the player's coordinates:

```javascript
const pixel = ctx.getImageData(xPos + 1, yPos, 1, 1).data;
if (pixel[0] > 150 && pixel[1] > 150 && pixel[2] > 150) { 
    // Collision Logic 
}

Note: We sample xPos + 1 to ensure collisions are caught even at high "Streak" speeds.
Game State Management

    initLevel(): Resets positions and triggers the procedural generation of obstacles.

    startCountdown(): A non-blocking timer that allows the player to visualize the path before movement begins.

    gameLoop(): Managed via requestAnimationFrame for smooth 60FPS performance.

🚀 Getting Started

    Clone the repository.

    Open index.html in any modern web browser.

    No external dependencies or local servers are required.

👥 Contribution & Development

This project was developed as a collaborative effort. Each major feature (Collision Engine, Level Scaling, Speed Streaks) was developed, reviewed, and committed by the team to ensure code quality and adherence to "Clean Code" principles.

    Team Note: This documentation was initially scaffolded via AI and subsequently validated and refined by the development team for technical accuracy.