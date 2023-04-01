/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples.paper1;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.io.File;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


/**
 * A simple example showing how to create
 * two datacenters with one host each and
 * run two cloudlets on them.
 */
public class DriverPaper1 {

    private static List<PatientData> patientList;

    /**
     * The cloudlet list.
     */
    private static List<CustomCloudlet> cloudletList;

    /**
     * The vmlist.
     */
    private static List<MyVm> vmlist;

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {

        Log.printLine("Starting Paper1...");

        try {
            // First s	tep: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the GridSim library
            CloudSim.init(num_user, calendar, trace_flag);


            //VM description
            int vmid = 0;
            int mips = 100;
            long size = 10000; //image size (MB)
            int ram = 512; //vm memory (MB)
            long bw = 1000;
            int pesNumber = 1; //number of cpus
            String vmm = "Xen"; //VMM name

            // Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            @SuppressWarnings("unused")
            Datacenter datacenter = createDatacenter("Datacenter_Apna", mips, ram, size, bw, Constant1.MACHINES);

            //Third step: Create Broker
            DataCenterBroker1 broker = createBroker();
            int brokerId = broker.getId();

            //Fourth step: Create one virtual machine
            vmlist = new ArrayList<MyVm>();

//            //create two VMs
//            Vm vm1 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
//
//            vmid++;
//            Vm vm2 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
//
//            //add the VMs to the vmList
//            vmlist.add(vm1);
//            vmlist.add(vm2);

            //create 100 VMs
            for (int i = 0; i < Constant1.SPECIAL_MACHINES; i++) {
                MyVm vm = new MyVm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared(), Constant1.PRIORITY_SPECIAL);
                vmlist.add(vm);
                vmid++;
            }

            for (int i = Constant1.SPECIAL_MACHINES; i < Constant1.MACHINES; i++) {
                MyVm vm = new MyVm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared(), Constant1.PRIORITY_NORMAL);
                vmlist.add(vm);
                vmid++;
            }


            //submit vm list to the broker
            broker.submitVmList(vmlist);

            patientList = CSVReader.readCSV("./modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/paper1/patient_entry.csv");

            //read CSV


            //Fifth step: Create two Cloudlets
            cloudletList = new ArrayList<CustomCloudlet>();

            //CustomCloudlet properties
//            int id = 0;
//            long lengthFactor = 10000;
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            for (PatientData patient : patientList) {
                double delay = (patient.patientTimeIn * (Constant1.LENGTH_FACTOR) / mips);
                //System.out.println("Delay: " + delay);
                CustomCloudlet cloudlet = new CustomCloudlet(patient.patientId, Constant1.LENGTH_FACTOR * patient.patientDuration, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, patient.patientPriority, delay);
                cloudlet.setUserId(brokerId);
                // System.out.println("VM ID OF CLOUDLET: " + cloudlet.getVmId());
                cloudletList.add(cloudlet);
            }

//            CustomCloudlet cloudlet1 = new CustomCloudlet(id, lengthFactor, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, Constant1.PRIORITY_SPECIAL);
//            cloudlet1.setUserId(brokerId);
//
//            id++;
//            CustomCloudlet cloudlet2 = new CustomCloudlet(id, lengthFactor, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, Constant1.PRIORITY_NORMAL);
//            cloudlet2.setUserId(brokerId);

            //add the cloudlets to the list
//            cloudletList.add(cloudlet1);
//            cloudletList.add(cloudlet2);

            //submit cloudlet list to the broker
            broker.submitCloudletList(cloudletList);


            //bind the cloudlets to the vms. This way, the broker
            // will submit the bound cloudlets only to the specific VM
//            broker.bindCloudletToVm(cloudlet1.getCloudletId(),vm1.getId());
//            broker.bindCloudletToVm(cloudlet2.getCloudletId(),vm2.getId());

            // Sixth step: Starts the simulation
            CloudSim.startSimulation();


            // Final step: Print results when simulation is over
            List<CustomCloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList, patientList);

            Log.printLine("Paper1 finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name, int mips, int ram, long storage, long bw, int multiplier) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        //    our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        //   int mips = 1000;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        //4. Create Host with its id and list of PEs and add them to the list of machines
        int hostId = 0;
//        int ram = 2048; //host memory (MB)
//        long storage = 1000000; //host storage
//        int bw = 10000;

        //in this example, the VMAllocatonPolicy in use is SpaceShared. It means that only one VM
        //is allowed to run on each Pe. As each Host has only one Pe, only one VM can run on each Host.

        for (int i = 0; i < multiplier; i++) {
            hostList.add(
                    new Host(
                            hostId,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList,
                            new VmSchedulerSpaceShared(peList)
                    )
            );
            hostId++;
        }

        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;        // the cost of using memory in this resource
        double costPerStorage = 0.001;    // the cost of using storage in this resource
        double costPerBw = 0.0;            // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();    //we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    //We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
    //to the specific rules of the simulated scenario
    private static DataCenterBroker1 createBroker() {

        DataCenterBroker1 broker = null;
        try {
            broker = new DataCenterBroker1("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the CustomCloudlet objects
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<CustomCloudlet> list, List<PatientData> patientDataList) {
        //Change std out to file
        try {
            File file = new File("./modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/paper1/output.txt");
            System.setOut(new PrintStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int size = list.size();
        CustomCloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        String outputHeading = "Cloudlet ID" + indent + "Cloudlet Length" + indent + "Priority" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "VM Type" + indent + "Time" + indent + "Start Time" + indent + "Finish Time";
        Log.printLine(outputHeading);

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent + indent + cloudlet.getCloudletLength() + indent + indent + indent + (cloudlet.getPriority() == 1 ? "High" : "Low ") + "  " + indent);

            if (cloudlet.getCloudletStatus() == CustomCloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() + indent + indent + indent + (vmlist.get(cloudlet.getVmId()).priority == 1 ? "High" : "Low ") +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }

        Log.printLine("\n**********************************************************************************\n");
        Log.printLine("Total number of cloudlets allocated: " + list.size());
        int specialAllocated=0;
        for(int i=0;i<list.size();i++){
            if(list.get(i).getPriority()==Constant1.PRIORITY_SPECIAL)
                specialAllocated++;
        }
        Log.printLine("Total number of high priority cloudlets allocated: " + specialAllocated);
        Log.printLine("Total number of low priority cloudlets allocated: " + (list.size()-specialAllocated));

        Log.printLine("Total number of cloudlets unallocated: " + (patientDataList.size()-list.size()));


        //Prinitng to File
//        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println(outputHeading);

//        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            System.out.print(indent + cloudlet.getCloudletId() + indent + indent + indent + cloudlet.getCloudletLength() + indent + indent + indent + (cloudlet.getPriority() == 1 ? "High" : "Low ") + "  " + indent);

            if (cloudlet.getCloudletStatus() == CustomCloudlet.SUCCESS) {
                System.out.print("SUCCESS");

                System.out.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() + indent + indent + indent + (vmlist.get(cloudlet.getVmId()).priority == 1 ? "High" : "Low ") +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }

        System.out.println("\n**********************************************************************************\n");
        System.out.println("Total number of cloudlets allocated: " + list.size());
//        int specialAllocated=0;
//        for(int i=0;i<list.size();i++){
//            if(list.get(i).getPriority()==Constant1.PRIORITY_SPECIAL)
//               specialAllocated++;
//        }
        System.out.println("Total number of high priority cloudlets allocated: " + specialAllocated);
        System.out.println("Total number of low priority cloudlets allocated: " + (list.size()-specialAllocated));
        System.out.println("Total number of cloudlets unallocated: " + (patientDataList.size()-list.size()));

    }

}
