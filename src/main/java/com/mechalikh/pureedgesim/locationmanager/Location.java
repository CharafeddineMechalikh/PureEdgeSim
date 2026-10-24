/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.locationmanager;

public class Location {
	protected double xPos;
	protected double yPos;

	public Location(double xPos, double yPos) {
		this.xPos = xPos;
		this.yPos = yPos;
	}

	public double getXPos() {
		return xPos;
	}

	public double getYPos() {
		return yPos;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Location other = (Location) o;
		return (this.xPos == other.xPos && this.yPos == other.yPos);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = hash(hash, toBits(this.xPos));
		return hash(hash, toBits(this.yPos));
	}

	protected int hash(final int hash, final int value) {
		return 89 * hash + value;
	}

	protected int toBits(final double value) {
		return (int) (Double.doubleToLongBits(value) ^ (Double.doubleToLongBits(value) >>> 32));
	}
}
