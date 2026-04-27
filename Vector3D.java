package SwingRenderer;

class Vector3D
{
	double x, y, z;

	public Vector3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vector3D(Vector3D vect)
	{
		this.x = vect.x;
		this.y = vect.y;
		this.z = vect.z;
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
	public double dot(Vector3D v)
	{
		return 	x * v.x + y * v.y + z * v.z;
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

	static Vector3D rotateAroundAxis(Vector3D v, Vector3D axis, double angle) 
	{
	    Vector3D k = axis.normalize();
	    double cos = Math.cos(angle), sin = Math.sin(angle);
	    // Rodrigues: v' = v*cosθ + (k×v)*sinθ + k*(k·v)*(1-cosθ)
	    Vector3D term1 = v.scale(cos);
	    Vector3D term2 = k.cross(v).scale(sin);
	    Vector3D term3 = k.scale(k.dot(v) * (1.0 - cos));
	    return term1.add(term2).add(term3);
	}

	public static Vector3D rotateAroundPoint(Vector3D vect, Vector3D pivot, double pitch, double yaw, double roll)
	{
		Vector3D translated = vect.subtract(pivot);
		Vector3D rotated = rotateXYZ(translated, pitch, yaw, roll);
		return rotated.add(pivot);
	}
	public Vector3D scale(double scaleFactor)
	{
		return new Vector3D(x*scaleFactor,y*scaleFactor,z*scaleFactor);
	}
	public float length() 
    {
        return (float) Math.sqrt(x * x + y * y + z*z);
    }
}