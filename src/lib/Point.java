package lib;

public class Point implements Cloneable{
	private double x;
	private double y;
	
	public Point(double x, double y){
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}
	
	public String toString(){
		return "[" + x + ", " + y + "]";
	}
	
	@Override
	public Point clone(){
		return new Point(x, y);
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof Point)){
			return false;
		}
		if(obj == this){
			return true;
		}
		
		Point p = (Point)obj;
		return p.x == x && p.y == y;
	}
}
