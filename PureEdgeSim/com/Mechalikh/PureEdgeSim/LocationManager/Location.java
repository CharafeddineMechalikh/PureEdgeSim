package com.Mechalikh.PureEdgeSim.LocationManager;

public class Location {
	private double xPos;
	private double yPos;
	public Location( double _xPos, double _yPos){
		xPos = _xPos;
		yPos = _yPos;
	}
	  
	public boolean equals(Location otherLocation){  
	    if (otherLocation == this) return true; 
	    
	    if(this.xPos == otherLocation.xPos && this.yPos == otherLocation.yPos)
	    	return true;
	    
		return false;
 
	}

	
	public double getXPos(){
		return xPos;
	}
	
	public double getYPos(){
		return yPos;
	}
}

