package SwingRenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;package SwingRenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SimpleRenderer
{
	public static final int WIDTH = 1000, HEIGHT = 700;

	public static Model createHardCodedModel()
	{
		Model cube = new Model();

		// flat to the screen
		cube.addPoint(-1, -1, -1);
		cube.addPoint(1, -1, -1);
		cube.addPoint(-1, 1, -1);
		cube.addPoint(1, 1, -1);
		cube.addPoint(-1, -1, -1);
		cube.addPoint(1, -1, -1);
		cube.addPoint(-1, -1, 1);
		cube.addPoint(1, -1, 1);
		cube.addPoint(-1, 1, 1); // index 8
		cube.addPoint(1, 1, 1); // index 9
		cube.addTri(0, 1, 2, Color.red);

		cube.addTri(3, 1, 2, new Color(255, 100, 100));

		// facing out away from the camera (bottom)
		cube.addTri(4, 5, 6, Color.blue);
		cube.addTri(7, 5, 6, new Color(100, 100, 255));

		// top
		cube.addTri(2, 3, 8, Color.green);
		cube.addTri(9, 3, 8, new Color(100, 255, 100));

		// back
		cube.addTri(8, 9, 6, Color.cyan);
		cube.addTri(9, 6, 7, new Color(100, 255, 255));

		// left
		cube.addTri(3, 1, 9, Color.MAGENTA);
		cube.addTri(9, 1, 7, new Color(255, 100, 255));
		return cube;
	}

	public static void main(String[] args) throws IOException
	{
		// Model model = createHardCodedModel();
		Model model = readObjFile("test");

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		RenderPanel panel = new RenderPanel(WIDTH, HEIGHT);
		panel.addModel(model);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);

		frame.addKeyListener(panel.camera);

		// the cube spins on 1 of its axis (roll) while the camera rotates around it
		// (changing yaw - if cameraOrbit is true)
		panel.loop();
	}

	/**
	 * Currently set to read only the diffuse (and alpha) of the current color materials in the MTL 
	 * From the Obj file it can read the vertexes and the faces.
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static Model readObjFile(String fileName) throws IOException
	{
		Model model = new Model();
		File file = new File(fileName);

		// read the MTL:
		/*
		 * newmtl Material.001 Ns 250.000000 Ka 1.000000 1.000000 1.000000 Kd 0.800212
		 * 0.000000 0.079430 Ks 0.500000 0.500000 0.500000 Ke 0.000000 0.000000 0.000000
		 * Ni 1.450000 d 1.000000 illum 2
		 * 
		 */
		HashMap<String, Color> map = new HashMap<>();
		BufferedReader mtl = new BufferedReader(new FileReader(file + ".mtl"));
		// find a line that says: newmtl Material.001
		while (true)
		{
			String line = mtl.readLine();
			if (line == null)
				break;
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("newmtl"))
			{
				String matName = line.split(" ")[1];
				line = mtl.readLine(); // Ns
				line = mtl.readLine(); // Ka (ambient color)
				line = mtl.readLine(); // Kd diffuse
				float r, g, b;
				String tokens[] = line.split(" ");
				r = Float.parseFloat(tokens[1]);
				g = Float.parseFloat(tokens[2]);
				b = Float.parseFloat(tokens[3]);
				line = mtl.readLine(); // Ks spec
				line = mtl.readLine(); // Ke emissive
				line = mtl.readLine(); // Ki (glass stuff?)
				line = mtl.readLine(); // D dissolve = alpha
				float a = Float.parseFloat(line.split(" ")[1]);
				map.put(matName, new Color(r, g, b, a));
			}
		}
		System.out.println("Color Map: " + map);

		BufferedReader br = new BufferedReader(new FileReader(file + ".obj"));
		Color currentColor = Color.magenta;
		while (true)
		{
			String line = br.readLine();
			if (line == null)
				break;
			System.out.println(line);
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("usemtl"))
			{
				// usemtl Material.001
				// open the material texture library and find the one name this:
				String name = line.split(" ")[1];
				currentColor = map.get(name);
			}
			if (line.startsWith("v "))
			{
				System.out.println("Vertex: " + line);
				String tokens[] = line.split(" ");
				model.addPoint(tokens[1], tokens[2], tokens[3]);
			}
			else if (line.startsWith("f "))
			{
				// f 5/1/1 3/2/1 1/3/1
				String tokens[] = line.split(" ");
				int v1 = Integer.parseInt(tokens[1].split("/")[0]) - 1; // -1 because face indexes vertexes starting at
																		// 1 instead of 0
				int v2 = Integer.parseInt(tokens[2].split("/")[0]) - 1;
				int v3 = Integer.parseInt(tokens[3].split("/")[0]) - 1;
				model.addTri(v1, v2, v3, currentColor);
				System.out.println("Face: " + line);
			}

		}
		return model;
	}
}

class RenderPanel extends JPanel
{
	static final Vector3D origin = new Vector3D(0f, 0f, 0f);
	private ArrayList<Model> renderList = new ArrayList<>();
	public static final float speed = .05f;

	private double zbuffer[][] = new double[SimpleRenderer.WIDTH][SimpleRenderer.HEIGHT];

	private void clearZBuffer()
	{
		// Fill with far plane (e.g., 1000f)
		for (int x = 0; x < SimpleRenderer.WIDTH; x++)
		{
			Arrays.fill(zbuffer[x], 1000f);
		}
	}

	private final BufferedImage frame;

	private void putPixel(int x, int y, int argb)
	{
		frame.setRGB(x, y, argb);
	}

	Camera camera = new Camera();

	public RenderPanel(int x, int y)
	{
		camera.setPostion(new Vector3D(0, 0, -5));
		camera.lookAt(origin);

		this.setSize(new Dimension(x, y));
		this.setPreferredSize(new Dimension(x, y));

		frame = new BufferedImage(SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT, BufferedImage.TYPE_INT_ARGB);

	}

	public void addModel(Model m)
	{
		renderList.add(m);
	}

	public void loop()
	{

		while (true)
		{
			// Manual Rotation/Orbit the modle
			// System.out.println(orbitAngle+" " +camera.position);
			// System.out.printf("yaw=%.3f rad, pitch=%.3f rad | degrees yaw=%.1f°,
			// pitch=%.1f°\n",
			// camera.yaw, camera.pitch,
			// Math.toDegrees(camera.yaw), Math.toDegrees(camera.pitch));

			// modify camera facing
			// camera.modifyPosition(0,0,.1f);
			// renderList.get(0).modifyRotation(.01f, 0, 0);
			
			this.repaint();
			try
			{
				Thread.sleep(30);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// paint component without zbuffer
	public void paintComponent(Graphics graphics)
	{
		if (camera.renderDirty)
		{
			// clear the view (just a faster way to clear the screen)
			final int bg = 0xFF000000; // opaque black
			DataBufferInt db = (DataBufferInt) frame.getRaster().getDataBuffer();
			int[] pixels = db.getData(); // length = W * H
			Arrays.fill(pixels, bg); // fills entire image in one call

			// clear the zbuffer every draw call
			clearZBuffer();

			paintTriangles_with_z_buffer();
			camera.renderDirty = false;
		}
		
		//getting the degrees for the debug output
		double yaw = Math.atan2(camera.forward.x, camera.forward.z);
		double pitch = Math.asin(-camera.forward.y);
		
		// debug drawing
		Graphics g = frame.getGraphics();
		g.setColor(Color.YELLOW);
		g.drawString("pos: " + camera.position, 10, 85);
		g.drawString("yaw: " + Math.toDegrees(yaw), 10, 100);
		g.drawString("pitch: " + Math.toDegrees(pitch), 10, 115);

		graphics.drawImage(frame, 0, 0, null);
	}

	//Generic paint triangles without considering the depth at which the triangle is at
	public void paintTriangles_no_z_buffer(Graphics g)
	{
		for (Model m : renderList)
		{
			m.projectVertices(camera, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);

			for (Model.Triangle tri : m.getTris())
			{
				Vector3D p1 = m.projectedVerts.get(tri.v1);
				Vector3D p2 = m.projectedVerts.get(tri.v2);
				Vector3D p3 = m.projectedVerts.get(tri.v3);

				g.setColor(tri.color);
				Polygon poly = new Polygon();
				poly.addPoint((int) p1.x, (int) p1.y);
				poly.addPoint((int) p2.x, (int) p2.y);
				poly.addPoint((int) p3.x, (int) p3.y);

				g.fillPolygon(poly);
			}
		}
	}

	public void paintTriangles_with_z_buffer()
	{
		//drawGrid(zbuffer, /* halfExtent */ 30, /* spacing */ 1f, new Color(180, 80, 180));

		// System.out.println(renderList.get(0).verts.get(3));
		for (Model m : renderList)
		{
			m.projectVertices(camera, 1000, 700);

			for (Model.Triangle tri : m.getTris())
			{
				// backface culling
				Vector3D normal = tri.getNormal();
				// Vector3D camDir = camera

				// System.out.println("Drawing tri: "+tri.v1+" "+tri.v2+" "+tri.v3);
				Vector3D p1 = m.projectedVerts.get(tri.v1);
				Vector3D p2 = m.projectedVerts.get(tri.v2);
				Vector3D p3 = m.projectedVerts.get(tri.v3);

				// these points were behind the camera so marked as null
				if (p1 == null || p2 == null || p3 == null)
					continue;

				// g.setColor(tri.color);
				int rgb = tri.color.getRGB();

				// bounding box (where do we have to look for pixels)
				int minX = (int) Math.max(0, Math.ceil(Math.min(p1.x, Math.min(p2.x, p3.x))));
				int maxX = (int) Math.min(SimpleRenderer.WIDTH - 1, Math.floor(Math.max(p1.x, Math.max(p2.x, p3.x))));
				int minY = (int) Math.max(0, Math.ceil(Math.min(p1.y, Math.min(p2.y, p3.y))));
				int maxY = (int) Math.min(SimpleRenderer.HEIGHT - 1, Math.floor(Math.max(p1.y, Math.max(p2.y, p3.y))));

				// scan through the triangle's bounding box to look for pixels that are in the
				// triangle
				for (int y = minY; y <= maxY; y++)
				{
					for (int x = minX; x <= maxX; x++)
					{
						double denom = ((p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.y - p3.y));
						double alpha = ((p2.y - p3.y) * (x - p3.x) + (p3.x - p2.x) * (y - p3.y)) / denom;
						double beta = ((p3.y - p1.y) * (x - p3.x) + (p1.x - p3.x) * (y - p3.y)) / denom;
						double gamma = 1.0 - alpha - beta;

						if (alpha >= 0 && beta >= 0 && gamma >= 0) // then it is in the bounding box
						{
							double z = alpha * p1.z + beta * p2.z + gamma * p3.z;

							if (z < zbuffer[x][y])
							{
								zbuffer[x][y] = z;
								// g.setColor(tri.color); // This is where lighting can be calculated. I would
								// need to setup a light, but we also need to consider whether the light is just
								// a directional light, or a point light
								// g.drawLine(x, y, x, y); // sets pixel
								frame.setRGB(x, y, rgb);
							}
						}
					}
				}
				//edge drawing:

				drawLineZ(p1, p2, 0xFF000000); // black outline 0xFF000000
				drawLineZ(p2, p3, 0xFF000000);
				drawLineZ(p3, p1, 0xFF000000);

				//draw line from
					//p1->p2
					//p2->p3
					//p3->p1
			}
		}
	}
	void drawLineZ(Vector3D a, Vector3D b, int rgb)
	{
	    int x0 = (int) Math.round(a.x);
	    int y0 = (int) Math.round(a.y);
	    int x1 = (int) Math.round(b.x);
	    int y1 = (int) Math.round(b.y);

	    int dx = Math.abs(x1 - x0);
	    int dy = Math.abs(y1 - y0);
	    int sx = x0 < x1 ? 1 : -1;
	    int sy = y0 < y1 ? 1 : -1;

	    int err = dx - dy;

	    int steps = Math.max(dx, dy);
	    double z0 = a.z;
	    double z1 = b.z;
	    int step = 0;

	    while (true)
	    {
	        if (x0 >= 0 && x0 < SimpleRenderer.WIDTH && y0 >= 0 && y0 < SimpleRenderer.HEIGHT)
	        {
	            double t = steps == 0 ? 0.0 : (double) step / steps;
	            double z = z0 * (1.0 - t) + z1 * t;
	            z -= 1e-6;
	            //System.out.println(z +" vs "+zbuffer[x0][y0]);
	            if (z < zbuffer[x0][y0])
	            {
	                zbuffer[x0][y0] = z;
	                frame.setRGB(x0, y0, rgb);
	            }
	        }

	        if (x0 == x1 && y0 == y1) break;

	        int e2 = 2 * err;
	        if (e2 > -dy) { err -= dy; x0 += sx; }
	        if (e2 < dx)  { err += dx; y0 += sy; }

	        step++;
	    }
	}


	private void drawGrid(double[][] zbuffer, int halfExtent, float spacing, Color lineColor)
	{

		// Parallel to X (vary Z)
		for (int iz = -halfExtent; iz <= halfExtent; iz++)
		{
			float z = iz * spacing;
			Vector3D a = new Vector3D(-halfExtent * spacing, 0f, z);
			Vector3D b = new Vector3D(+halfExtent * spacing, 0f, z);

			// Transform to view & project
			Vector3D va = camera.worldToView(a);
			Vector3D vb = camera.worldToView(b);
			if (va.z <= 0 || vb.z <= 0)
				continue; // simple near-plane reject

			Vector3D pa = Model.projectToScreen(va, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);
			Vector3D pb = Model.projectToScreen(vb, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);

			drawLineZ(pa, pb, zbuffer, lineColor);
		}

		// Parallel to Z (vary X)
		for (int ix = -halfExtent; ix <= halfExtent; ix++)
		{
			float x = ix * spacing;
			Vector3D a = new Vector3D(x, 0f, -halfExtent * spacing);
			Vector3D b = new Vector3D(x, 0f, +halfExtent * spacing);

			Vector3D va = camera.worldToView(a);
			Vector3D vb = camera.worldToView(b);

			Segment s = clipLineToNear(va, vb);
			if (s == null)
				continue;

			Vector3D pa = Model.projectToScreen(s.a, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);
			Vector3D pb = Model.projectToScreen(s.b, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);

			Segment clipped = clipLineToViewport(pa, pb, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);
			if (clipped == null)
				continue;

			drawLineZ(pa, pb, zbuffer, lineColor);
		}
	}

	private static final int INSIDE = 0; // 0000
	private static final int LEFT = 1; // 0001
	private static final int RIGHT = 2; // 0010
	private static final int BOTTOM = 4; // 0100
	private static final int TOP = 8; // 1000

	private int computeOutCode(double x, double y, int w, int h)
	{
		int code = INSIDE;
		if (x < 0)
			code |= LEFT;
		else if (x >= w)
			code |= RIGHT;
		if (y < 0)
			code |= TOP; // note: y grows down, so top is y<0
		else if (y >= h)
			code |= BOTTOM;
		return code;
	}

	/**
	 * Clips p1(x1,y1,z1), p2(x2,y2,z2) to viewport [0,w) x [0,h). Preserves z by
	 * linear interpolation. Returns null if fully outside.
	 */
	private Segment clipLineToViewport(Vector3D p1, Vector3D p2, int w, int h)
	{
		double x1 = p1.x, y1 = p1.y, z1 = p1.z;
		double x2 = p2.x, y2 = p2.y, z2 = p2.z;

		int out1 = computeOutCode(x1, y1, w, h);
		int out2 = computeOutCode(x2, y2, w, h);

		while (true)
		{
			if ((out1 | out2) == 0)
			{
				// Trivially accepted
				return new Segment(new Vector3D(x1, y1, z1), new Vector3D(x2, y2, z2));
			}
			else if ((out1 & out2) != 0)
			{
				// Trivially rejected
				return null;
			}
			else
			{
				// Choose an endpoint outside the rectangle
				int out = (out1 != 0) ? out1 : out2;
				double x = 0, y = 0, z = 0;

				// Parametric: p(t) = P1 + t*(P2-P1)
				double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;

				if ((out & TOP) != 0)
				{ // y < 0
					double t = (0 - y1) / dy;
					x = x1 + t * dx;
					y = 0;
					z = z1 + t * dz;
				}
				else if ((out & BOTTOM) != 0)
				{ // y >= h
					double t = (h - 1 - y1) / dy;
					x = x1 + t * dx;
					y = h - 1;
					z = z1 + t * dz;
				}
				else if ((out & RIGHT) != 0)
				{ // x >= w
					double t = (w - 1 - x1) / dx;
					x = w - 1;
					y = y1 + t * dy;
					z = z1 + t * dz;
				}
				else if ((out & LEFT) != 0)
				{ // x < 0
					double t = (0 - x1) / dx;
					x = 0;
					y = y1 + t * dy;
					z = z1 + t * dz;
				}

				// Replace the endpoint and recompute its outcode
				if (out == out1)
				{
					x1 = x;
					y1 = y;
					z1 = z;
					out1 = computeOutCode(x1, y1, w, h);
				}
				else
				{
					x2 = x;
					y2 = y;
					z2 = z;
					out2 = computeOutCode(x2, y2, w, h);
				}
			}
		}
	}

	// View-space near plane (same convention as your renderer: z > 0 is in front)
	private static final double NEAR = 1e-3; // tiny epsilon to avoid exact zero

	/**
	 * Clips a line segment AB to z>=NEAR in view space. Returns null if the segment
	 * is entirely behind the near plane.
	 */
	private Segment clipLineToNear(Vector3D A, Vector3D B)
	{
		double zA = A.z, zB = B.z;

		// Both behind the near plane -> drop
		if (zA <= NEAR && zB <= NEAR)
			return null;

		Vector3D a = new Vector3D(A.x, A.y, A.z);
		Vector3D b = new Vector3D(B.x, B.y, B.z);

		// If A behind, move A to intersection with z=NEAR
		if (zA <= NEAR && zB > NEAR)
		{
			double t = (NEAR - zA) / (zB - zA); // t in (0,1]
			a = new Vector3D(A.x + t * (B.x - A.x), A.y + t * (B.y - A.y), NEAR);
		}

		// If B behind, move B to intersection with z=NEAR
		if (zB <= NEAR && zA > NEAR)
		{
			double t = (NEAR - zA) / (zB - zA); // t in (0,1]
			b = new Vector3D(A.x + t * (B.x - A.x), A.y + t * (B.y - A.y), NEAR);
		}

		return new Segment(a, b);
	}

	static class Segment
	{
		Vector3D a, b;

		Segment(Vector3D a, Vector3D b)
		{
			this.a = a;
			this.b = b;
		}
	}

	private void drawLineZ(Vector3D p1, Vector3D p2, double[][] zbuffer, Color color)
	{
		// p1, p2 are *screen-space* points: x,y in pixels; z is view-space depth (> 0
		// for your pipeline)
		int x1 = (int) p1.x, y1 = (int) p1.y;
		int x2 = (int) p2.x, y2 = (int) p2.y;

		int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
		int sx = x1 < x2 ? 1 : -1;
		int sy = y1 < y2 ? 1 : -1;
		int err = dx - dy;

		int steps = Math.max(dx, dy);
		// To avoid divide-by-zero, clamp to at least 1
		steps = Math.max(steps, 1);

		// Linear interpolation of depth along the line in screen space
		double z1 = p1.z, z2 = p2.z;
		double dz = (z2 - z1) / steps;

		int x = x1, y = y1;
		double z = z1;

		int rgb = color.getRGB();

		for (int i = 0; i < 5000; i++) // 5000 is a max check amount
		{
			// System.out.println("drawLineZ");
			if (x >= 0 && x < SimpleRenderer.WIDTH && y >= 0 && y < SimpleRenderer.HEIGHT)
			{
				// Depth test: lower z (closer) wins
				if (z < zbuffer[x][y])
				{
					zbuffer[x][y] = z;
					frame.setRGB(x, y, rgb); // set pixel
				}
			}
			if (x == x2 && y == y2)
				break;

			int e2 = 2 * err;
			if (e2 > -dy)
			{
				err -= dy;
				x += sx;
			}
			if (e2 < dx)
			{
				err += dx;
				y += sy;
			}
			z += dz;
		}
	}

}

class Model
{
	private ArrayList<Vector3D> verts = new ArrayList<>();
	protected ArrayList<Vector3D> projectedVerts; // After each transformation, we need to recalculate these - they are where the
										// verts are on the 2D screen

	private double rotations[] = new double[3]; // yaw, pitch, roll

	class Triangle
	{
		int v1, v2, v3; // indexes of the vert
		Color color; // later texture

		public Triangle(int v1, int v2, int v3, Color color)
		{
			super();
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.color = color;
		}

		public Vector3D getNormal()
		{
			Vector3D a = verts.get(v1);
			Vector3D b = verts.get(v2);
			Vector3D c = verts.get(v3);

			Vector3D ab = b.subtract(a);
			Vector3D ac = c.subtract(a);
			Vector3D normal = ab.cross(ac).normalize();
			return normal;
		}
	}

	private ArrayList<Triangle> tris = new ArrayList<>();

	public void addPoint(Vector3D vect)
	{
		verts.add(vect);
	}

	public ArrayList<Triangle> getTris()
	{
		return tris;
	}

	public void addPoint(String x, String y, String z)
	{
		float fx = Float.parseFloat(x);
		float fy = Float.parseFloat(y);
		float fz = Float.parseFloat(z);
		verts.add(new Vector3D(fx, fy, fz));
	}

	public void addPoint(int x, int y, int z)
	{
		verts.add(new Vector3D(x, y, z));
	}

	public void addTri(int p1, int p2, int p3, Color color)
	{
		// TODO: check to make sure the points are valid
		tris.add(new Triangle(p1, p2, p3, color));
	}

	public void projectVertices(Camera cam, int screenWidth, int screenHeight)
	{
		projectedVerts = new ArrayList<>(verts.size());
		for (Vector3D v : verts)
		{
			Vector3D viewSpace = cam.worldToView(v);
			if (viewSpace.z <= 0)
			{
				projectedVerts.add(null); // Mark as not drawable
				continue;
			}
			Vector3D projected = projectToScreen(viewSpace, screenWidth, screenHeight);
			projectedVerts.add(projected);
		}
	}

	public static Vector3D projectToScreen(Vector3D p, int screenWidth, int screenHeight)
	{
		double fov = Math.toRadians(70);
		double aspect = screenWidth / (double) screenHeight;
		double f = 1.0 / Math.tan(fov / 2);

		double x_proj = (f / aspect) * (p.x / p.z);
		double y_proj = f * (p.y / p.z);

		int screenX = (int) ((x_proj + 1) * screenWidth / 2);
		int screenY = (int) ((1 - y_proj) * screenHeight / 2);

		return new Vector3D(screenX, screenY, p.z); // keep depth in z
	}
}

class Camera implements KeyListener
{
	//TODO: Fields should be protected but I'm not sure where and how will need access right now
	Vector3D position;
	Vector3D forward;

	final float moveAmt = .1f;
	boolean renderDirty = true;

	public Camera()
	{
		forward = new Vector3D(0, 0, 1);
	}

	public void modifyPosition(float x, float y, float z)
	{
		position.x += x;
		position.y += y;
		position.z += z;
	}

	public void setPostion(Vector3D vect)
	{
		position = new Vector3D(vect.x, vect.y, vect.z);
	}

	public Vector3D worldToView(Vector3D worldPoint)
	{
		// subtract position and rotate inverse of camera transform
		return transformCamera(worldPoint, position, this);
	}

	public void setForward(Vector3D newForward)
	{
		// if we set forward, assume that up is world coordinate up:
		forward = new Vector3D(newForward).normalize();
	}

	public void lookAt(Vector3D target)
	{
		setForward(target.subtract(position)); // normalized inside setForward
	}

	public final static Vector3D world_up = new Vector3D(0, 1, 0);

	public Vector3D getRight()
	{

		// Right-handed basis: right = up × forward
		Vector3D r = world_up.cross(forward);
		// If forward ~ WORLD_UP, fall back to a different up to avoid degeneracy
		if (r.length() < 1e-6)
		{
			Vector3D altUp = new Vector3D(1, 0, 0);
			r = altUp.cross(forward);
		}
		return r.normalize();

		// return forward.cross(new Vector3D(0,1,0)).normalize();
	}

	public Vector3D getUp()
	{
		return getRight().cross(forward).normalize();
	}

	public static Vector3D transformCamera(Vector3D worldPoint, Vector3D camPos, Camera camera)
	{
		Vector3D forward = camera.forward.normalize();
		Vector3D right = camera.getRight();
		Vector3D up = camera.getUp();

		Vector3D rel = worldPoint.subtract(camPos);

		double camX = rel.dot(right); // horizontal
		double camY = rel.dot(up); // vertical
		double camZ = rel.dot(forward); // depth

		return new Vector3D(camX, camY, camZ);

	}

	public Vector3D getForward()
	{
		return forward.normalize();
	}

	@Override
	public void keyPressed(KeyEvent a)
	{
		System.out.println(a.getKeyChar() + "was pressed");
		if (a.getKeyCode() == KeyEvent.VK_T) // up
		{
			position = position.add(getUp().scale(-moveAmt));
		}
		if (a.getKeyCode() == KeyEvent.VK_G) // Down
		{
			position = position.add(getUp().scale(moveAmt));
		}
		if (a.getKeyCode() == KeyEvent.VK_W)
		{
//			position.x -= dirX * moveAmt;
//			position.y -= dirY * moveAmt;
//			position.z -= dirZ * moveAmt;
			position = position.add(getForward().scale(moveAmt));
		}
		if (a.getKeyCode() == KeyEvent.VK_S)
		{
//			position.x += dirX * moveAmt;
//			position.y += dirY * moveAmt;
//			position.z += dirZ * moveAmt;
			position = position.add(getForward().scale(-moveAmt));
		}
		if (a.getKeyCode() == KeyEvent.VK_A)
		{
//		    position.x -= rightX * moveAmt;
//		    position.z -= rightZ * moveAmt;
			position = position.add(getRight().scale(-moveAmt));
		}
		if (a.getKeyCode() == KeyEvent.VK_D)
		{
//		    position.x += rightX * moveAmt;
//		    position.z += rightZ * moveAmt;
			position = position.add(getRight().scale(moveAmt));
		}
		if (a.getKeyCode() == KeyEvent.VK_Q)
		{
			setForward(Vector3D.rotateAroundAxis(forward, world_up, -Math.toRadians(15)));
		}
		if (a.getKeyCode() == KeyEvent.VK_E)
		{
			setForward(Vector3D.rotateAroundAxis(forward, world_up, Math.toRadians(15)));
		}
		if (a.getKeyCode() == KeyEvent.VK_R)
			setForward(Vector3D.rotateAroundAxis(forward, getRight(), Math.toRadians(15)));
		if (a.getKeyCode() == KeyEvent.VK_F)
			setForward(Vector3D.rotateAroundAxis(forward, getRight(), -Math.toRadians(15)));

		renderDirty = true;
	}

	@Override
	public void keyReleased(KeyEvent arg0)
	{}

	@Override
	public void keyTyped(KeyEvent arg0)
	{}
}

public class SimpleRenderer
{
	public static final int WIDTH = 1000, HEIGHT = 700;
	public static void main(String[] args)
	{
		Model cube = new Model();

		// flat to the screen
		cube.addPoint(-1, -1, -1);
		cube.addPoint(1, -1, -1);
		cube.addPoint(-1, 1, -1);
		cube.addPoint(1, 1, -1);
		cube.addPoint(-1, -1, -1);
		cube.addPoint(1, -1, -1);
		cube.addPoint(-1, -1, 1);
		cube.addPoint(1, -1, 1);
		cube.addPoint(-1, 1, 1); // index 8
		cube.addPoint(1, 1, 1); // index 9
		cube.addTri(0, 1, 2, Color.red);

		cube.addTri(3, 1, 2, new Color(255, 100, 100));

		// facing out away from the camera (bottom)
		cube.addTri(4, 5, 6, Color.blue);
		cube.addTri(7, 5, 6, new Color(100, 100, 255));

		// top
		cube.addTri(2, 3, 8, Color.green);
		cube.addTri(9, 3, 8, new Color(100, 255, 100));

		// back
		cube.addTri(8, 9, 6, Color.cyan);
		cube.addTri(9, 6, 7, new Color(100, 255, 255));

		// left
		cube.addTri(3, 1, 9, Color.MAGENTA);
		cube.addTri(9, 1, 7, new Color(255, 100, 255));

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		RenderPanel panel = new RenderPanel(WIDTH, HEIGHT);
		panel.addModel(cube);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
		
		frame.addKeyListener(panel.camera);
		
		// the cube spins on 1 of its axis (roll) while the camera rotates around it (changing yaw - if cameraOrbit is true)
		panel.loop();
	}

}

class RenderPanel extends JPanel
{
	ArrayList<Model> renderList = new ArrayList<>();
	public static final float speed = .05f;

	Camera camera = new Camera();
	boolean cameraOrbit = true;
	public RenderPanel(int x, int y)
	{
		camera.setPostion(new Vector3D(0, 0, -5));
		this.setSize(new Dimension(x, y));
		this.setPreferredSize(new Dimension(x, y));
	}
	public void addModel(Model m)
	{
		renderList.add(m);
	}
	public void loop()
	{
		Vector3D origin = new Vector3D(0.0f, 0, 0.0f);
		final float deltaAngle = speed;
		float orbitAngle = 0;

		//camera position - look at the origin
		double radius = 5.0;
		double theta = orbitAngle; // changes over time to orbit
		double camY = .5f; // fixed height above ground
		
		Vector3D toTarget = origin.subtract(camera.position); // from camera to (0,0,0)

		double yaw = Math.atan2(toTarget.x, toTarget.z);
		double pitch = Math.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
		

		camera.yaw = yaw;
		camera.pitch = pitch;
		
		while (true)
		{
			if (cameraOrbit)
			{
				// make camera orbit the model:
				orbitAngle += deltaAngle;
		
				// Orbit camera around target
				double camX = origin.x + radius * Math.cos(theta);
				double camZ = origin.z + radius * Math.sin(theta);
	
				camera.position = new Vector3D(camX, camY, camZ);
				
				//face the target as we orbit around
				toTarget = origin.subtract(camera.position); // from camera to (0,0,0)

				yaw = Math.atan2(toTarget.x, toTarget.z);
				pitch = Math.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
				

				camera.yaw = yaw;
				camera.pitch = pitch;
			}

			// System.out.println(orbitAngle+" " +camera.position);
			// System.out.printf("yaw=%.3f rad, pitch=%.3f rad | degrees yaw=%.1f°, pitch=%.1f°\n",
			// camera.yaw, camera.pitch,
			// Math.toDegrees(camera.yaw), Math.toDegrees(camera.pitch));

			// modify camera facing
			


			renderList.get(0).modifyRotation(.01f, 0, 0);
			this.repaint();
			try
			{
				Thread.sleep(30);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	// paint component without zbuffer
	public void paintComponent(Graphics g)
	{

		// clear the panel
		g.setColor(Color.black);
		g.fillRect(0, 0, 1000, 700);

		g.setColor(Color.YELLOW);
		g.drawString("yaw:" + Math.toDegrees(camera.yaw), 10, 100);

		paintTriangles_with_z_buffer(g);

	}
	public void paintTriangles_no_z_buffer(Graphics g)
	{

		for (Model m : renderList)
		{
			m.projectVertices(camera, SimpleRenderer.WIDTH, SimpleRenderer.HEIGHT);

			for (Model.Triangle tri : m.tris)
			{
				Vector3D p1 = m.projectedVerts.get(tri.v1);
				Vector3D p2 = m.projectedVerts.get(tri.v2);
				Vector3D p3 = m.projectedVerts.get(tri.v3);

				g.setColor(tri.color);
				Polygon poly = new Polygon();
				poly.addPoint((int) p1.x, (int) p1.y);
				poly.addPoint((int) p2.x, (int) p2.y);
				poly.addPoint((int) p3.x, (int) p3.y);

				g.fillPolygon(poly);

			}
		}
	}
	public void paintTriangles_with_z_buffer(Graphics g)
	{
		double zbuffer[][] = new double[SimpleRenderer.WIDTH][SimpleRenderer.HEIGHT];
		for (int x = 0; x < zbuffer.length; x++)
		{
			Arrays.fill(zbuffer[x], 1000); // 1000 is the far plane (to technically this should come from and be stored in the camera class)
		}
		System.out.println(renderList.get(0).verts.get(3));
		for (Model m : renderList)
		{
			m.projectVertices(camera, 1000, 700);

			for (Model.Triangle tri : m.tris)
			{
				Vector3D p1 = m.projectedVerts.get(tri.v1);
				Vector3D p2 = m.projectedVerts.get(tri.v2);
				Vector3D p3 = m.projectedVerts.get(tri.v3);

				g.setColor(tri.color);

				// bounding box (where do we have to look for pixels)
				int minX = (int) Math.max(0, Math.ceil(Math.min(p1.x, Math.min(p2.x, p3.x))));
				int maxX = (int) Math.min(SimpleRenderer.WIDTH - 1, Math.floor(Math.max(p1.x, Math.max(p2.x, p3.x))));
				int minY = (int) Math.max(0, Math.ceil(Math.min(p1.y, Math.min(p2.y, p3.y))));
				int maxY = (int) Math.min(SimpleRenderer.HEIGHT - 1, Math.floor(Math.max(p1.y, Math.max(p2.y, p3.y))));

				//scan through the triangle's bounding box to look for pixels that are in the triangle
				for (int y = minY; y <= maxY; y++)
				{
					for (int x = minX; x <= maxX; x++)
					{
						double denom = ((p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.y - p3.y));
						double alpha = ((p2.y - p3.y) * (x - p3.x) + (p3.x - p2.x) * (y - p3.y)) / denom;
						double beta = ((p3.y - p1.y) * (x - p3.x) + (p1.x - p3.x) * (y - p3.y)) / denom;
						double gamma = 1.0 - alpha - beta;

						if (alpha >= 0 && beta >= 0 && gamma >= 0) //then it is in the bounding box
						{
							double z = alpha * p1.z + beta * p2.z + gamma * p3.z;

							if (z < zbuffer[x][y])
							{
								zbuffer[x][y] = z;
								// g.setColor(tri.color); // This is where lighting can be calculated. I would need to setup a light, but we also need to consider whether the light is just a directional light, or a point light
								g.drawLine(x, y, x, y); // sets pixel
							}
						}
					}
				}
			}
		}
	}
}

class Model
{
	ArrayList<Vector3D> verts = new ArrayList<>();
	ArrayList<Vector3D> projectedVerts; //After each transformation, we need to recalculate these - they are where the verts are on the 2D screen

	double rotations[] = new double[3]; // yaw, pitch, roll

	class Triangle
	{
		int v1, v2, v3; // indexes of the vert
		Color color; // later texture

		public Triangle(int v1, int v2, int v3, Color color)
		{
			super();
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.color = color;
		}

		public Vector3D getNormal()
		{
			Vector3D a = verts.get(v1);
			Vector3D b = verts.get(v2);
			Vector3D c = verts.get(v3);

			Vector3D ab = b.subtract(a);
			Vector3D ac = c.subtract(a);
			Vector3D normal = ab.cross(ac).normalize();
			return normal;
		}
	}

	ArrayList<Triangle> tris = new ArrayList<>();

	public void modifyRotation(float yaw, float pitch, float roll)
	{
		rotations[0] += yaw;
		rotations[1] += pitch;
		rotations[2] += roll;

		// update model
		updateModelVerts();

		rotations[0] = 0;
		rotations[1] = 0;
		rotations[2] = 0;
	}

	private void updateModelVerts()
	{
		double yaw = rotations[0];
		double pitch = rotations[1];
		double roll = rotations[2];

		for (Vector3D v : verts)
		{
			double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
			double x1 = cosY * v.x + sinY * v.z;
			double z1 = -sinY * v.x + cosY * v.z;

			// Apply pitch (X-axis)
			double cosX = Math.cos(pitch), sinX = Math.sin(pitch);
			double y2 = cosX * v.y - sinX * z1;
			double z2 = sinX * v.y + cosX * z1;

			// Apply roll (Z-axis)
			double cosZ = Math.cos(roll), sinZ = Math.sin(roll);
			double x3 = cosZ * x1 - sinZ * y2;
			double y3 = sinZ * x1 + cosZ * y2;
			// System.out.println("Before: "+v);
			v.setValues(x3, y3, z2);
			// System.out.println("After: "+v);
		}

	}

	public void addPoint(Vector3D vect)
	{
		verts.add(vect);
	}

	public void addPoint(int x, int y, int z)
	{
		verts.add(new Vector3D(x, y, z));
	}

	public void addTri(int p1, int p2, int p3, Color color)
	{
		// TODO: check to make sure the points are valid
		tris.add(new Triangle(p1, p2, p3, color));
	}

	public void projectVertices(Camera cam, int screenWidth, int screenHeight)
	{
		projectedVerts = new ArrayList<>(verts.size());
		for (Vector3D v : verts)
		{
			Vector3D viewSpace = cam.worldToView(v);
			Vector3D projected = projectToScreen(viewSpace, screenWidth, screenHeight);
			projectedVerts.add(projected);
		}
	}

	private Vector3D projectToScreen(Vector3D p, int screenWidth, int screenHeight)
	{
		double fov = Math.toRadians(70);
		double aspect = screenWidth / (double) screenHeight;
		double f = 1.0 / Math.tan(fov / 2);

		double x_proj = (f / aspect) * (p.x / p.z);
		double y_proj = f * (p.y / p.z);

		int screenX = (int) ((x_proj + 1) * screenWidth / 2);
		int screenY = (int) ((1 - y_proj) * screenHeight / 2);

		return new Vector3D(screenX, screenY, p.z); // keep depth in z
	}
}

class Vector3D
{
	double x, y, z;

	public Vector3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public void setValues(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public String toString()
	{
		return "(" + x + "," + y + "," + z + ")";
	}

	public Vector3D subtract(Vector3D other)
	{
		return new Vector3D(x - other.x, y - other.y, z - other.z);
	}
	public Vector3D add(Vector3D other)
	{
		return new Vector3D(x + other.x, y + other.y, z + other.z);
	}

	public Vector3D cross(Vector3D v)
	{
		return new Vector3D(
				y * v.z - z * v.y,
				z * v.x - x * v.z,
				x * v.y - y * v.x);
	}

	public Vector3D normalize()
	{
		double len = Math.sqrt(x * x + y * y + z * z);
		if (len == 0)
			return new Vector3D(0, 0, 0);
		return new Vector3D(x / len, y / len, z / len);
	}

	//Note though that the rotations are being processed in order, so any pitch changes will effect how the yaw works...
	public static Vector3D rotateXYZ(Vector3D v, double pitch, double yaw, double roll)
	{
		// Apply yaw (Y-axis)
		double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
		double x1 = cosY * v.x + sinY * v.z;
		double z1 = -sinY * v.x + cosY * v.z;

		// Apply pitch (X-axis)
		double cosX = Math.cos(pitch), sinX = Math.sin(pitch);
		double y2 = cosX * v.y - sinX * z1;
		double z2 = sinX * v.y + cosX * z1;

		// Apply roll (Z-axis)
		double cosZ = Math.cos(roll), sinZ = Math.sin(roll);
		double x3 = cosZ * x1 - sinZ * y2;
		double y3 = sinZ * x1 + cosZ * y2;

		return new Vector3D(x3, y3, z2);
	}
	public static Vector3D rotateAroundPoint(Vector3D vect, Vector3D pivot, double pitch, double yaw, double roll)
	{
		Vector3D translated = vect.subtract(pivot);
		Vector3D rotated = rotateXYZ(translated, pitch, yaw, roll);
		return rotated.add(pivot);
	}
}

class Camera implements KeyListener
{
	Vector3D position;
	double yaw, pitch;
	
	float moveAmt = 1f;

	public Vector3D worldToView(Vector3D worldPoint)
	{
		// subtract position and rotate inverse of camera transform
		return transformCamera(worldPoint, position, yaw, pitch);
	}
	public void setPostion(Vector3D vect)
	{
		position = new Vector3D(vect.x, vect.y, vect.z);
	}
	public void setYaw(double yaw)
	{
		this.yaw = yaw;
	}
	public void setPitch(double pitch)
	{
		this.pitch = pitch;
	}
	public static Vector3D transformCamera(Vector3D worldPoint, Vector3D camPos, double yaw, double pitch)
	{
		// Step 1: Translate point relative to camera
		double x = worldPoint.x - camPos.x;
		double y = worldPoint.y - camPos.y;
		double z = worldPoint.z - camPos.z;

		// Step 2: Yaw rotation (around Y axis)
		double cosYaw = Math.cos(yaw);
		double sinYaw = Math.sin(yaw);
		double x1 = cosYaw * x - sinYaw * z;
		double z1 = sinYaw * x + cosYaw * z;

		// Step 3: Pitch rotation (around X axis)
		double cosPitch = Math.cos(-pitch);
		double sinPitch = Math.sin(-pitch);
		double y1 = cosPitch * y - sinPitch * z1;
		double z2 = sinPitch * y + cosPitch * z1;

		return new Vector3D(x1, y1, z2);
	}
	@Override
	public void keyPressed(KeyEvent a)
	{
		if (a.getKeyCode() == KeyEvent.VK_W)
			position.z+=moveAmt;
		if (a.getKeyCode() == KeyEvent.VK_A)
			position.x-=moveAmt;
		if (a.getKeyCode() == KeyEvent.VK_S)
			position.z-=moveAmt;
		if (a.getKeyCode() == KeyEvent.VK_D)
			position.x+=moveAmt;
		if (a.getKeyCode() == KeyEvent.VK_Q)
			yaw+=.03f;
		if (a.getKeyCode() == KeyEvent.VK_E)
			yaw-=.03f;
	}
	@Override
	public void keyReleased(KeyEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	public void keyTyped(KeyEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}
}
