package com.mechalikh.pureedgesim.TasksOrchestration;

import com.mechalikh.pureedgesim.DataCentersManager.EdgeVM;
import com.mechalikh.pureedgesim.TasksGenerator.Task;

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
