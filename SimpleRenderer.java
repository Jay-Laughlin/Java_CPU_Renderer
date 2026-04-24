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
import javax.swing.JPanel;

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