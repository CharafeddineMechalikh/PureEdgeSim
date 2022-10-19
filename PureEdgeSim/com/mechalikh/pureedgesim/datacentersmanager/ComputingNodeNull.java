package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.LinkedList;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * A class that implements the Null Object Design Pattern for the
 * {@link ComputingNode} class. Needed to avoid {@link NullPointerException}
 * when using the NULL object instead of attributing null to ComputingNode
 * variables.
 */
public class ComputingNodeNull implements ComputingNode {

	@Override
	public void submitTask(Task task) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public TYPES getType() {
		return null;
	}

	@Override
	public void setType(TYPES type) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public void setName(String name) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public boolean isOrchestrator() {
		return false;
	}

	@Override
	public void setAsOrchestrator(boolean isOrchestrator) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public ComputingNode getOrchestrator() {
		return ComputingNode.NULL;
	}

	@Override
	public void enableTaskGeneration(boolean generateTasks) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public boolean isGeneratingTasks() {
		return false;
	}

	@Override
	public NetworkLink getCurrentUpLink() {
		return NetworkLink.NULL;
	}

	@Override
	public void setCurrentUpLink(NetworkLink currentUpLink) {
	}

	@Override
	public NetworkLink getCurrentDownLink() {
		return NetworkLink.NULL;
	}

	@Override
	public void setCurrentDownLink(NetworkLink currentDownLink) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public NetworkLink getCurrentWiFiLink() {
		return NetworkLink.NULL;
	}

	@Override
	public void setCurrentWiFiLink(NetworkLink currentWiFiDeviceToDeviceLink) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public boolean isDead() {
		return false;
	}

	@Override
	public double getDeathTime() {
		return -1;
	}

	@Override
	public EnergyModelComputingNode getEnergyModel() {
		return EnergyModelComputingNode.NULL;
	}

	@Override
	public void setEnergyModel(EnergyModelComputingNode energyModel) {
	}

	@Override
	public MobilityModel getMobilityModel() {
		return MobilityModel.NULL;
	}

	@Override
	public void setMobilityModel(MobilityModel mobilityModel) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public boolean isPeripheral() {
		return false;
	}

	@Override
	public void setPeriphery(boolean periphery) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public ComputingNode getApplicationPlacementLocation() {
		return ComputingNode.NULL;
	}

	@Override
	public boolean isApplicationPlaced() {
		return false;
	}

	@Override
	public void setApplicationPlaced(boolean isApplicationPlaced) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public double getNumberOfCPUCores() {
		return 0;
	}

	@Override
	public void setNumberOfCPUCores(int numberOfCPUCores) {
	}

	@Override
	public int getApplicationType() {
		return -1;
	}

	@Override
	public void setApplicationType(int applicationType) {
	}

	@Override
	public double getAvailableStorage() {
		return 0;
	}

	@Override
	public void setAvailableStorage(double availableStorage) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public double getAvgCpuUtilization() {
		return 0;
	}

	@Override
	public double getCurrentCpuUtilization() {
		return 0;
	}

	@Override
	public boolean isIdle() {
		return false;
	}

	@Override
	public void setIdle(boolean isIdle) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public void addCpuUtilization(Task task) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public void removeCpuUtilization(Task task) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public boolean isSensor() {
		return false;
	}

	@Override
	public void setAsSensor(boolean isSensor) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public LinkedList<Task> getTasksQueue() {
		return new LinkedList<Task>();
	}

	@Override
	public double getTotalStorage() {
		return 0;
	}

	@Override
	public void setStorage(double storage) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public double getTotalMipsCapacity() {
		return 0;
	}

	@Override
	public void setTotalMipsCapacity(double totalMipsCapacity) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public double getMipsPerCore() {
		return 0;
	}

	@Override
	public int getId() {
		return -1;
	}

	@Override
	public double getRamCapacity() { 
		return 0;
	}

	@Override
	public double getAvailableRam() { 
		return 0;
	}

	@Override
	public void setRam(double ram) { 
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public void setAvailableRam(double ram) {
		// Do nothing since this Computing Node is supposed to be null.
	}

	@Override
	public void setOrchestrator(ComputingNode orchestrator) {
		// Do nothing since this Computing Node is supposed to be null.	
	}


}
