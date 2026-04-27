# Java_CPU_Renderer
<p align="center">
  <img width="600" height="500" alt="image" src="https://github.com/user-attachments/assets/de22605f-0d8a-4c96-81cf-718362b5d29e" />
</p>

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

<p align="center">
<img width="746" height="498" alt="{718117D8-EFF3-4E2A-9D48-9B6836456D50}" src="https://github.com/user-attachments/assets/05b4c7dd-6277-44f8-b72b-2f0398fc2a69" />
</p>

Reading in a simple .obj model (verts and faces) and the diffuse out of the .mtl file working.
I also added edge drawing (future will removing inner edges of coplaner faces)
