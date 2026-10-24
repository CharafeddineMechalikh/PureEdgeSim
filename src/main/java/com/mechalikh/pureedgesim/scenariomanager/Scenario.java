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

/**
 * One simulation configuration: an orchestration algorithm, an orchestration
 * architecture, and a number of edge devices. Scenario sweeps combine every
 * algorithm with every architecture and device count.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public class Scenario {
	/** Number of edge devices in this scenario. */
	protected int devicesCount;
	/** Index of the architecture in {@link SimulationParameters#orchestrationArchitectures}. */
	protected int orchArchitecture;
	/** Index of the algorithm in {@link SimulationParameters#orchestrationAlgorithms}. */
	protected int orchAlgorithm;

	/**
	 * Creates a scenario.
	 *
	 * @param devicesCount    number of edge devices
	 * @param orchAlgorithm   index of the orchestration algorithm
	 * @param orchArchitecture index of the orchestration architecture
	 */
	public Scenario(int devicesCount, int orchAlgorithm, int orchArchitecture) {
		this.setOrchAlgorithm(orchAlgorithm);
		this.setDevicesCount(devicesCount);
		this.setOrchArchitecture(orchArchitecture);
	}

	/**
	 * Returns the index of the orchestration algorithm.
	 *
	 * @return the algorithm index
	 */
	public int getOrchAlgorithm() {
		return orchAlgorithm;
	}

	/**
	 * Sets the index of the orchestration algorithm.
	 *
	 * @param orchAlgorithm the algorithm index
	 */
	public void setOrchAlgorithm(int orchAlgorithm) {
		this.orchAlgorithm = orchAlgorithm;
	}

	/**
	 * Returns the index of the orchestration architecture.
	 *
	 * @return the architecture index
	 */
	public int getOrchArchitecture() {
		return orchArchitecture;
	}

	/**
	 * Sets the index of the orchestration architecture.
	 *
	 * @param orchArchitecture the architecture index
	 */
	public void setOrchArchitecture(int orchArchitecture) {
		this.orchArchitecture = orchArchitecture;
	}

	/**
	 * Returns the number of edge devices.
	 *
	 * @return the device count
	 */
	public int getDevicesCount() {
		return devicesCount;
	}

	/**
	 * Sets the number of edge devices.
	 *
	 * @param devicesCount the device count
	 */
	public void setDevicesCount(int devicesCount) {
		this.devicesCount = devicesCount;
	}

	/**
	 * Returns the architecture name resolved from the configured architecture
	 * list.
	 *
	 * @return the architecture name, without surrounding whitespace
	 */
	public String getStringOrchArchitecture() { 
		return SimulationParameters.orchestrationArchitectures[orchArchitecture].trim();
	}

	/**
	 * Returns the algorithm name resolved from the configured algorithm list.
	 *
	 * @return the algorithm name, without surrounding whitespace
	 */
	public String getStringOrchAlgorithm() {
		return SimulationParameters.orchestrationAlgorithms[orchAlgorithm].trim();
	}

	/**
	 * Returns a human-readable description of the scenario, used in logs.
	 *
	 * @return the algorithm, architecture, and device count
	 */
	public String toString() {
		return "Orchestration algorithm= " + getStringOrchAlgorithm()
				+ " -  Architecture= " + getStringOrchArchitecture() + " -  number of edge devices= "
				+ getDevicesCount();
	}
}
