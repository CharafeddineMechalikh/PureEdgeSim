package examples;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;

/**
 * Task generator that synchronizes the requests of the whole fleet.
 * <p>
 * The default generator spreads the tasks of each minute over the first fifteen
 * seconds of that minute, so the bursts of different devices rarely overlap.
 * This generator starts every minute at the same instant for every device,
 * which models a fleet that reports on the minute mark (meter readings, clock
 * synchronization, scheduled e-health uploads) and makes all devices contend for
 * the network and the servers at the same moment.
 * <p>
 * Applications that declare an arrival pattern keep the default behaviour,
 * because their requests are not generated per minute.
 *
 * @since PureEdgeSim 6.0
 */
public class Example23BurstTaskGenerator extends DefaultTaskGenerator {

	/**
	 * Creates a task generator for the given simulation.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example23BurstTaskGenerator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Generates the tasks of one device. Requests without an arrival pattern are
	 * generated once per minute, all aligned on the minute mark.
	 *
	 * @param dev the device to generate tasks for
	 * @param app the application type
	 */
	@Override
	protected void generateTasksForDevice(ComputingNode dev, int app) {
		Application application = SimulationParameters.applicationList.get(app);
		if (application.hasRandomArrival()) {
			super.generateTasksForDevice(dev, app);
			return;
		}
		for (int minute = 0; minute < (int) simulationTime; minute++)
			insert(minute * 60, app, dev);
	}
}
