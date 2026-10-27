package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class TransferProgressTest {

	@Test
	void constructorCopiesTaskSizeAndType() {
		Task task = mock(Task.class);
		TransferProgress transfer = new TransferProgress(task, 1024, TransferProgress.Type.REQUEST);

		assertSame(task, transfer.getTask());
		assertEquals(1024, transfer.getFileSize());
		assertEquals(1024, transfer.getRemainingFileSize());
		assertEquals(TransferProgress.Type.REQUEST, transfer.getTransferType());
		assertEquals(0, transfer.getCurrentBandwidth());
		assertEquals(0, transfer.getAverageBandwidth());
	}

	@Test
	void remainingFileSizeCanBeUpdated() {
		TransferProgress transfer = new TransferProgress(mock(Task.class), 100,
				TransferProgress.Type.TASK);
		transfer.setRemainingFileSize(25);
		assertEquals(25, transfer.getRemainingFileSize());
	}

	@Test
	void networkUsagesCanBeUpdatedIndependently() {
		TransferProgress transfer = new TransferProgress(mock(Task.class), 100,
				TransferProgress.Type.TASK);
		transfer.setWanNetworkUsage(1.5);
		transfer.setManNetworkUsage(2.5);
		transfer.setLanNetworkUsage(3.5);
		assertEquals(1.5, transfer.getWanNetworkUsage());
		assertEquals(2.5, transfer.getManNetworkUsage());
		assertEquals(3.5, transfer.getLanNetworkUsage());
	}

	@Test
	void currentBandwidthAlsoTracksAverage() {
		TransferProgress transfer = new TransferProgress(mock(Task.class), 100,
				TransferProgress.Type.TASK);
		transfer.setCurrentBandwidth(100);
		assertEquals(100, transfer.getCurrentBandwidth());
		assertEquals(100, transfer.getAverageBandwidth());
		transfer.setCurrentBandwidth(300);
		assertEquals(300, transfer.getCurrentBandwidth());
		assertEquals(200, transfer.getAverageBandwidth());
	}

	@Test
	void vertexAndEdgeListsCanBeSetFluently() {
		TransferProgress transfer = new TransferProgress(mock(Task.class), 100,
				TransferProgress.Type.CONTAINER);
		List<ComputingNode> vertices = new ArrayList<>();
		vertices.add(ComputingNode.NULL);
		List<NetworkLink> edges = new ArrayList<>();
		NetworkLink link = new NetworkLink();
		edges.add(link);

		assertSame(transfer, transfer.setVertexList(vertices));
		assertSame(transfer, transfer.setEdgeList(edges));
		assertSame(vertices, transfer.getVertexList());
		assertSame(edges, transfer.getEdgeList());
		assertSame(link, transfer.getEdgeList().get(0));
	}

	@Test
	void allTransferTypesAreAvailable() {
		assertEquals(5, TransferProgress.Type.values().length);
		assertEquals(TransferProgress.Type.TASK, TransferProgress.Type.valueOf("TASK"));
		assertEquals(TransferProgress.Type.CONTAINER, TransferProgress.Type.valueOf("CONTAINER"));
		assertEquals(TransferProgress.Type.RESULTS_TO_DEV, TransferProgress.Type.valueOf("RESULTS_TO_DEV"));
		assertEquals(TransferProgress.Type.RESULTS_TO_ORCH, TransferProgress.Type.valueOf("RESULTS_TO_ORCH"));
		assertEquals(TransferProgress.Type.REQUEST, TransferProgress.Type.valueOf("REQUEST"));
	}
}
