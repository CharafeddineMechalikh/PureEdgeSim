package com.Mechalikh.PureEdgeSim.TasksOrchestration;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class VmTaskMapItem {

	private Task task;
	private EdgeVM vm;

	public VmTaskMapItem(EdgeVM vm, Task task) {
		 this.setVm(vm);
		 this.setTask(task);
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public EdgeVM getVm() {
		return vm;
	}

	public void setVm(EdgeVM vm) {
		this.vm = vm;
	}

}
