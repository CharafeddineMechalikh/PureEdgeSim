package com.Mechalikh.PureEdgeSim.Network;
  
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class taskTransferProgress {
private Task task;
private double remainingFileSize;
private double wanBandwidth;
private double lanBandwidth; 
private double WanNetworkUsage=0;
private double LanNetworkUsage=0;
private boolean executable;
public taskTransferProgress(Task task, double remainingFileSize, boolean executable) {
	this.task=task;
	this.remainingFileSize=remainingFileSize;
	this.executable=executable;
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
public boolean isExecutable() {
	return executable;
}
public void setExecutable(boolean executable) {
	this.executable = executable;
}

}
