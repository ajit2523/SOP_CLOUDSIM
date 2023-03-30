package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.List;

public class CustomCloudlet extends Cloudlet {


    private final int priority;
    private final double delayCloudlet;
    public CustomCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, int priority, double delayCloudlet) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.priority = priority;
        this.delayCloudlet = delayCloudlet;
    }

    public int getPriority() {
        return priority;
    }

    public double getDelayCloudlet() {
        return delayCloudlet;
    }
}