package examples;

import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Computing nodes generator that never drops a declared device type.
 * <p>
 * The default allocation distributes the fleet with the largest remainder
 * method, which is fair to the declared shares but can give zero devices to a
 * rare type. In a small scenario a 2 % share of 20 devices rounds to zero, so
 * the scenario would silently run without that device. This generator keeps the
 * default allocation, then reserves one device for every type with a positive
 * share that received none, taking it from the type with the most devices.
 *
 * @since PureEdgeSim 6.0
 */
public class Example23TypeGuaranteeGenerator extends DefaultComputingNodesGenerator {

	/**
	 * Creates a generator for the given simulation.
	 *
	 * @param simulationManager  the simulation manager
	 * @param mobilityModelClass the mobility model class used for every node
	 * @param computingNodeClass the computing node class used for every node
	 */
	public Example23TypeGuaranteeGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		super(simulationManager, mobilityModelClass, computingNodeClass);
	}

	/**
	 * Allocates the devices with the default largest remainder method, then
	 * reserves one device for every declared type that would otherwise be absent.
	 *
	 * @param percentages the percentage of each device type, in file order
	 * @return the number of instances to create for each type, in file order
	 */
	@Override
	protected int[] allocateInstances(List<Double> percentages) {
		int[] allocation = super.allocateInstances(percentages);
		for (int type = 0; type < allocation.length; type++) {
			if (percentages.get(type) <= 0 || allocation[type] > 0)
				continue;
			int donor = largestAllocation(allocation);
			if (donor < 0)
				break;
			allocation[donor]--;
			allocation[type] = 1;
			getSimulationManager().getSimulationLogger().print(
					"%s - Device type %d declares %s %% of the fleet, a share that rounds to zero: one device reserved from type %d",
					getClass().getSimpleName(), type + 1, percentages.get(type), donor + 1);
		}
		return allocation;
	}

	/**
	 * Finds the type with the most devices, provided it has more than one so that
	 * it keeps at least one of its own.
	 *
	 * @param allocation the current allocation
	 * @return the index of the donor type, or -1 when no device can be spared
	 */
	private static int largestAllocation(int[] allocation) {
		int donor = -1;
		for (int type = 0; type < allocation.length; type++)
			if (allocation[type] > 1 && (donor < 0 || allocation[type] > allocation[donor]))
				donor = type;
		return donor;
	}
}
