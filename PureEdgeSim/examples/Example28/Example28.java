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
package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * A battery powered Mist fleet that runs out of power, and what placing work by
 * remaining autonomy is worth against placing it by speed.
 * <p>
 * Forty fixed devices, all of them on a battery, all of them generating two
 * requests a minute and all of them able to compute. The scenario is
 * {@code MIST_ONLY} with no separate orchestrator, so the only decision the fleet
 * makes is whose battery pays for whose task. The batteries are sized against the
 * run: each one holds between sixteen and thirty minutes of the energy its device
 * draws while idle, while the run lasts fifteen minutes. No device can therefore die
 * of staying awake, and a device only goes when the work it was handed pushes its
 * drain past what it holds. Measured on this scenario the fleet burns 32 watt-hours,
 * 24 of them doing nothing and 8 of them computing, and the 8 are the only thing a
 * placement policy can move. Which devices end up carrying them is the whole
 * question: a device that goes takes its queued tasks with it and never generates the
 * ones it had left.
 * <p>
 * Two switches:
 * <ul>
 * <li>{@code -Dexample28.batteryAware=false} ranks the candidates by their
 * estimated delay instead of by the autonomy that would be left to them once they
 * have paid for the task. The fast devices win every contest, so they carry the
 * fleet and die first.</li>
 * <li>{@code -Dexample28.perTask=false} lets the framework reuse the first placement
 * of each device for all of its later tasks, which turns one decision into a
 * commitment to serve one neighbour for the whole run and gathers the work again.</li>
 * </ul>
 * <p>
 * {@link Example28FleetReporter} prints what each run is worth: how many devices
 * survived and when the fleet fell, how many requests died with their device, and
 * how many decisions the placement cache saved.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example28BatteryAwareOrchestrator
 * @see Example28FleetReporter
 */
public class Example28 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example28/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example28/output/";

	/**
	 * Whether work is placed on the device that can afford it longest. Read at every
	 * decision so that a test can run both arms of the ablation in one JVM.
	 *
	 * @return true unless the switch is set to false
	 */
	public static boolean batteryAware() {
		return !"false".equals(System.getProperty("example28.batteryAware"));
	}

	/**
	 * Whether each task is placed again, or the framework reuses the first
	 * placement of the device.
	 *
	 * @return true unless the switch is set to false
	 */
	public static boolean perTask() {
		return !"false".equals(System.getProperty("example28.perTask"));
	}

	/**
	 * Runs the draining fleet with the custom orchestrator and the reporter that
	 * counts the deaths.
	 */
	public Example28() {
		Simulation sim = new Simulation();

		sim.setCustomEdgeOrchestrator(Example28BatteryAwareOrchestrator.class);
		sim.setCustomSimulationManager(Example28FleetReporter.class);
		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);
		sim.launchSimulation();
	}

	/**
	 * Starts the example.
	 *
	 * @param args ignored; the switches are the {@code -Dexample28.batteryAware} and
	 *             {@code -Dexample28.perTask} system properties, passed before the
	 *             main class
	 */
	public static void main(String[] args) {
		new Example28();
	}
}
