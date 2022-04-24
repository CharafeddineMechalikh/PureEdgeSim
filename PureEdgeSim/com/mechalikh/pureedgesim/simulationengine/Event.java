package com.mechalikh.pureedgesim.simulationengine;
 

public class Event implements Comparable<Event> {
	double time;
	private SimEntity simEntity;
	private int tag;
	private Object data;
	private long serial; 

	public Event(SimEntity simEntity, Double time, int tag) {
		this.simEntity = simEntity;
		this.time = time;
		this.tag = tag;
	}

	public Event(SimEntity simEntity, Double time, int tag, Object data) {
		this.simEntity = simEntity;
		this.time = time;
		this.tag = tag;
		this.data = data;
	}

	public int getTag() {
		return tag;
	}

	public double getTime() {
		return time;
	}

	public SimEntity getSimEntity() {
		return simEntity;
	}

	public Object getData() {
		return data;
	}
 
	public void setSerial(long l) { 
		this.serial=l;
	}


    @Override
    public int compareTo(final Event that) {
        if (that == null || that ==null) {
            return 1;
        }

        if (this == that) {
            return 0;
        }

        int res = Double.compare(time, that.getTime());
        if (res != 0) {
            return res;
        }

        return Long.compare(serial, that.getSerial());
    }

	private long getSerial() { 
		return this.serial;
	}

}
