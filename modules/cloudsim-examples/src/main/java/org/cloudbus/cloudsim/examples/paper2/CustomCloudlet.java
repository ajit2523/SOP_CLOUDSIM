package org.cloudbus.cloudsim.examples.paper2;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class CustomCloudlet extends Cloudlet {

    private int priority;
    private double deadline;

    public CustomCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.priority = 0;
    }

    public void setDeadline(double deadline) {
        this.deadline = deadline;
    }

    public double getDeadline() {
        return this.deadline;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setRandomPriority(){
        int[] priorities = {1, 2, 3}; // 3 = High, 2 = Medium, 1 = Low
        int randomIndex = (int)(Math.random() * 3);
        this.priority = priorities[randomIndex];
    }
}
