package com.Mechalikh.PureEdgeSim.LocationManager;

public class Location {
	private int xPos;
	private int yPos;
	public Location( int _xPos, int _yPos){
		xPos = _xPos;
		yPos = _yPos;
	}
	 
	@Override
	public boolean equals(Object other){
		boolean result = false;
	    if (other == null) return false;
	    if (!(other instanceof Location))return false;
	    if (other == this) return true;
	    
	    Location otherLocation = (Location)other;
	    if(this.xPos == otherLocation.xPos && this.yPos == otherLocation.yPos)
	    	result = true;

	    return result;
	}

	
	public int getXPos(){
		return xPos;
	}
	
	public int getYPos(){
		return yPos;
	}
}

