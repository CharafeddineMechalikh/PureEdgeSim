package com.mechalikh.pureedgesim.taskgenerator;

import java.util.Objects;

public abstract class TaskAbstract implements Task, Comparable<Task> {

	protected double maxLatency = 0;
	protected double actualNetworkTime = 0;
	protected double execFinishTime = 0;
	protected double execStartTime = 0;
	protected double arrivalTime = 0;
	protected double length = 0;
	protected int id;
	protected long serial;

	protected TaskAbstract(int id) {
		this.setId(id);
	}

	@Override
	public double getMaxLatency() {
		return maxLatency;
	}

	@Override
	public void setMaxLatency(double maxLatency) {
		this.maxLatency = maxLatency;
	}

	@Override
	public double getActualNetworkTime() {
		return actualNetworkTime;
	}

	@Override
	public void addActualNetworkTime(double actualNetworkTime) {
		this.actualNetworkTime += actualNetworkTime;
	}

	@Override
	public double getActualCpuTime() {
		return this.execFinishTime - this.getExecStartTime();
	}

	@Override
	public double getExecStartTime() {
		return execStartTime;
	}

	@Override
	public double getWatingTime() {
		return this.execStartTime - this.arrivalTime;
	}

	@Override
	public void setArrivalTime(double clock) {
		this.arrivalTime = clock;
		this.execStartTime = clock;
	}
	
	@Override
	public double getTotalDelay() {
		return this.getActualNetworkTime() + this.getWatingTime() + this.getActualCpuTime(); 
	}

	@Override
	public void setExecutionStartTime(double clock) {
		this.execStartTime = clock;
		this.execFinishTime = clock;
	}

	@Override
	public void setExecutionFinishTime(double clock) {
		this.execFinishTime = clock;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public double getLength() {
		return length;
	}

	@Override
	public void setLength(double length) {
		this.length = length;
	}
	


	public void setSerial(long l) {
		this.serial = l;
	}

	@Override
	public int compareTo(final Task that) {
		if (that.equals(null)) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(this.getTime(), that.getTime());
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final Task that = (Task) obj;
		return Double.compare(that.getTime(), getTime()) == 0 
				&& getSerial() == that.getSerial();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTime(), getSerial());
	}

	public long getSerial() {
		return this.serial;
	}
}
