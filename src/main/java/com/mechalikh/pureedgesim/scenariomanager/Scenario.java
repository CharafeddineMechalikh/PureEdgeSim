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
 *     @author Charafeddien Mechalikh
 **/
package com.mechalikh.pureedgesim.scenariomanager;

public class Scenario {
	protected int devicesCount; // The number of edge devices in this scenario
	protected int orchArchitecture; // The used architecture/ computing paradigms
	protected int orchAlgorithm; // The tasks orchestration algorithm that will be used in this scenario

	public Scenario(int devicesCount, int orchAlgorithm, int orchArchitecture) {
		this.setOrchAlgorithm(orchAlgorithm);
		this.setDevicesCount(devicesCount);
		this.setOrchArchitecture(orchArchitecture);
	}

	public int getOrchAlgorithm() {
		return orchAlgorithm;
	}

	public void setOrchAlgorithm(int orchAlgorithm) {
		this.orchAlgorithm = orchAlgorithm;
	}

	public int getOrchArchitecture() {
		return orchArchitecture;
	}

	public void setOrchArchitecture(int orchArchitecture) {
		this.orchArchitecture = orchArchitecture;
	}

	public int getDevicesCount() {
		return devicesCount;
	}

	public void setDevicesCount(int devicesCount) {
		this.devicesCount = devicesCount;
	}

	public String getStringOrchArchitecture() { 
		return SimulationParameters.orchestrationArchitectures[orchArchitecture].trim();
	}

	public String getStringOrchAlgorithm() {
		return SimulationParameters.orchestrationAlgorithms[orchAlgorithm].trim();
	}

	public String toString() {
		return "Orchestration algorithm= " + getStringOrchAlgorithm()
				+ " -  Architecture= " + getStringOrchArchitecture() + " -  number of edge devices= "
				+ getDevicesCount();
	}
}
