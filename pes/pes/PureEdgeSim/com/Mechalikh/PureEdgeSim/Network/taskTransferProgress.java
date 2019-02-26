package com.Mechalikh.PureEdgeSim.Network;
  
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class taskTransferProgress {
private Task task;
private double remainingFileSize;
public static final int TASK = 0;
public static final int CONTAINER = 1;
public static final int RESULTS_TO_DEV = 2;
public static final int RESULTS_TO_ORCH = 4;
public static final int REQUEST = 5;
private double wanBandwidth;
private double lanBandwidth; 
private double WanNetworkUsage=0;
private double LanNetworkUsage=0;
private int type;
private double fileSize;
public taskTransferProgress(Task task, double remainingFileSize, int type) {
	this.task=task;
	this.remainingFileSize=remainingFileSize;
	this.setFileSize(remainingFileSize);
	this.type=type;
}
public double getRemainingFileSize() {
	return remainingFileSize;
}
public void setRemainingFileSize(double remainingFileSize) {
	this.remainingFileSize = remainingFileSize;
}
public Task getTask() {
	return task;
}
public void setTask(Task task) {
	this.task = task;
}
public double getWanBandwidth() {
	return wanBandwidth;
}
public void setWanBandwidth(double wanBandwidth) {
	this.wanBandwidth = wanBandwidth;
}
public double getLanBandwidth() {
	return lanBandwidth;
}
public void setLanBandwidth(double lanBandwidth) {
	this.lanBandwidth = lanBandwidth;
	
}
 
public double getWanNetworkUsage() {
	return WanNetworkUsage;
}
public void setWanNetworkUsage(double wanNetworkUsage) {
	WanNetworkUsage = wanNetworkUsage;
}
public double getLanNetworkUsage() {
	return LanNetworkUsage;
}
public void setLanNetworkUsage(double lanNetworkUsage) {
	LanNetworkUsage = lanNetworkUsage;
}
public int getType() {
	return type;
}
public void setType(int type) {
	this.type = type;
}
public double getFileSize() {
	// TODO Auto-generated method stub
	return fileSize;
}
public void setFileSize(double fileSize) {
	this.fileSize = fileSize;
}

}
