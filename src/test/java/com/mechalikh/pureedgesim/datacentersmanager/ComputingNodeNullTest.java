package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Verifies the Null Object implementation of {@link ComputingNode}: every
 * operation must be safe, side effect free, and return neutral values.
 */
class ComputingNodeNullTest extends DatacentersTestSupport {

	@Test
	void singletonIsExposedThroughComputingNodeConstant() {
		assertSame(ComputingNodeNull.getInstance(), ComputingNode.NULL);
		assertSame(ComputingNode.NULL, ComputingNodeNull.getInstance());
	}

	@Test
	void everyGetterReturnsItsNeutralValue() {
		ComputingNode nullNode = ComputingNode.NULL;

		assertEquals(TYPES.NULL, nullNode.getType());
		assertEquals("", nullNode.getName());
		assertFalse(nullNode.isOrchestrator());
		assertSame(ComputingNode.NULL, nullNode.getOrchestrator());
		assertFalse(nullNode.isGeneratingTasks());
		assertFalse(nullNode.isDead());
		assertEquals(-1, nullNode.getDeathTime());
		assertSame(EnergyModelComputingNode.NULL, nullNode.getEnergyModel());
		assertSame(MobilityModel.NULL, nullNode.getMobilityModel());
		assertFalse(nullNode.isPeripheral());
		assertSame(ComputingNode.NULL, nullNode.getApplicationPlacementLocation());
		assertFalse(nullNode.isApplicationPlaced());
		assertEquals(0, nullNode.getNumberOfCPUCores());
		assertEquals(-1, nullNode.getApplicationType());
		assertEquals(0, nullNode.getAvailableStorage());
		assertEquals(0, nullNode.getAvgCpuUtilization());
		assertEquals(0, nullNode.getCurrentCpuUtilization());
		assertFalse(nullNode.isIdle());
		assertFalse(nullNode.isSensor());
		assertEquals(0, nullNode.getTotalStorage());
		assertEquals(0, nullNode.getTotalMipsCapacity());
		assertEquals(0, nullNode.getMipsPerCore());
		assertEquals(-1, nullNode.getId());
		assertEquals(0, nullNode.getRamCapacity());
		assertEquals(0, nullNode.getAvailableRam());
		assertSame(NetworkLink.NULL, nullNode.getCurrentLink(LinkOrientation.UP_LINK));
		assertSame(NetworkLink.NULL, nullNode.getCurrentLink(LinkOrientation.DOWN_LINK));
		assertSame(NetworkLink.NULL, nullNode.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));

		List<Task> queue = nullNode.getTasksQueue();
		assertTrue(queue.isEmpty());
	}

	@Test
	void everyMutatorIsANoOp() {
		ComputingNode nullNode = ComputingNode.NULL;
		Task task = Mockito.mock(Task.class);

		nullNode.setType(TYPES.CLOUD);
		nullNode.setName("ignored");
		nullNode.setAsOrchestrator(true);
		nullNode.setOrchestrator(Mockito.mock(ComputingNode.class));
		nullNode.enableTaskGeneration(true);
		nullNode.setEnergyModel(Mockito.mock(EnergyModelComputingNode.class));
		nullNode.setMobilityModel(Mockito.mock(MobilityModel.class));
		nullNode.setPeriphery(true);
		nullNode.setApplicationPlacementLocation(Mockito.mock(ComputingNode.class));
		nullNode.setApplicationPlaced(true);
		nullNode.setNumberOfCPUCores(8);
		nullNode.setApplicationType(2);
		nullNode.setAvailableStorage(100);
		nullNode.setIdle(false);
		nullNode.addCpuUtilization(task);
		nullNode.removeCpuUtilization(task);
		nullNode.setAsSensor(true);
		nullNode.submitTask(task);
		nullNode.setStorage(100);
		nullNode.setTotalMipsCapacity(100);
		nullNode.setRam(100);
		nullNode.setAvailableRam(100);
		nullNode.setCurrentLink(Mockito.mock(NetworkLink.class), LinkOrientation.UP_LINK);

		// None of the calls above may change the neutral state nor throw.
		assertEquals(TYPES.NULL, nullNode.getType());
		assertEquals("", nullNode.getName());
		assertFalse(nullNode.isOrchestrator());
		assertFalse(nullNode.isGeneratingTasks());
		assertFalse(nullNode.isDead());
		assertSame(ComputingNode.NULL, nullNode.getApplicationPlacementLocation());
		assertFalse(nullNode.isApplicationPlaced());
		assertEquals(0, nullNode.getNumberOfCPUCores());
		assertEquals(-1, nullNode.getApplicationType());
		assertEquals(0, nullNode.getAvailableStorage());
		assertFalse(nullNode.isIdle());
		assertFalse(nullNode.isSensor());
		assertEquals(0, nullNode.getTotalStorage());
		assertEquals(0, nullNode.getTotalMipsCapacity());
		assertEquals(0, nullNode.getRamCapacity());
		assertEquals(0, nullNode.getAvailableRam());
		assertTrue(nullNode.getTasksQueue().isEmpty());
	}
}
