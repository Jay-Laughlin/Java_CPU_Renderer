# Java_CPU_Renderer
<img width="996" height="742" alt="image" src="https://github.com/user-attachments/assets/de22605f-0d8a-4c96-81cf-718362b5d29e" />

This project is a CPU-based software renderer created as a learning and teaching tool
to explore the mathematics and systems underlying real-time rendering.

The goal of this project was not performance or visual fidelity, but clarity:
making the core stages of the rendering pipeline visible and reasoned about without
relying on GPU shader stages or graphics APIs such as OpenGL or DirectX.

This renderer is intentionally not GPU-accelerated and is not intended for real-time
production use. Modern engines and APIs rightly abstract much of this work away;
this project exists to better explore what those abstractions are doing.

- Coordinate spaces and transformations (model, world, view, projection)
- Perspective projection mathematics
- View frustum clipping
- Triangle rasterization
- Depth buffering (z-buffer)
- Back-face culling
- Screen-space interpolation

Future goals are to continue to clean this up and add additional features to test different techniques.
