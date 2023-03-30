package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

public class MyVm extends Vm {
    private long maxCycles = Constant1.LENGTH_FACTOR * 60;
    private long cyclesCompleted = 0;

    public MyVm(int id, int userId, double mips, int pesNumber, int ram, long bw, long size, String vmm,
                CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, pesNumber, ram, bw, size, vmm, cloudletScheduler);
    }
}
