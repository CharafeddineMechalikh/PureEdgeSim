package examples;

import com.mechalikh.pureedgesim.DataCentersManager.EdgeDataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.EnergyModel;
import com.mechalikh.pureedgesim.Network.FileTransferProgress;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;

public class CustomEnergyModel extends EnergyModel {

	/*
	 * This class is an example of how to create a custom energy model The class
	 * needs to be inherited form the EnergyModel class.
	 */
	public CustomEnergyModel(double maxActiveConsumption, double idleConsumption) {
		super(maxActiveConsumption, idleConsumption);
	}

	@Override
	public void updateCpuEnergyConsumption(double cpuUtilization) {
		// Here we update the cpu consumption
		// if the CPU operates at 100% it will consumes the amount of
		// "maxActiveConsumption + the idle consumption every second
		double consumedEnergy = this.maxActiveConsumption * cpuUtilization + this.idleConsumption;

		// the cpu and energy update interval ( see the update_interval in simulation
		// parameters) can be set to any value , for example 0.1 second
		// however the conusmed energy that we calculated is for 1 seconds,
		// we need to multiply it by the interval in order to get the correct
		// consumption

		consumedEnergy = consumedEnergy * simulationParameters.UPDATE_INTERVAL;

		// add the results to the consumed energy
		this.cpuEnergyConsumption += consumedEnergy;
	}

	@Override
	public void updatewirelessEnergyConsumption(FileTransferProgress file, EdgeDataCenter device1,
			EdgeDataCenter device2, int flag) {
		// Here things gets complicated
		// to simplify this example, let's assume that's every transferred Kilobyte will
		// consume 0.0000001 Wh of energy
		// the file size is in Kbits, it needs to be converted to Bytes ( divided by 8)
		double filesize=  file.getFileSize()/8;
		
		//then the energy consumed during the transfer of this file is
		this.wirelessEnergyConsumption+= filesize* 0.0000001;
		
		// of course this model is oversimplified
		// for a more reaslitic model, please check the default PureEdgeSim Energy Model (DefaultEnergyModel) 
	}

}
