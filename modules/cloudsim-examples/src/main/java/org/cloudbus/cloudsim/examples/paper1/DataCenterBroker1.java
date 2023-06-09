/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples.paper1;

import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.lists.*;

/**
 * DatacentreBroker represents a broker acting on behalf of a user. It hides VM management, as vm
 * creation, sumbission of cloudlets to this VMs and destruction of VMs.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class DataCenterBroker1 extends SimEntity {

    /**
     * The vm list.
     */
    protected List<? extends MyVm> vmList;

    /**
     * The vms created list.
     */
    protected List<? extends MyVm> vmsCreatedList;

    /**
     * The cloudlet list.
     */
    protected List<? extends CustomCloudlet> cloudletList;

    /**
     * The cloudlet submitted list.
     */
    protected List<? extends CustomCloudlet> cloudletSubmittedList;

    /**
     * The cloudlet received list.
     */
    protected List<? extends CustomCloudlet> cloudletReceivedList;

    /**
     * The cloudlets submitted.
     */
    protected int cloudletsSubmitted;

    /**
     * The vms requested.
     */
    protected int vmsRequested;

    /**
     * The vms acks.
     */
    protected int vmsAcks;

    /**
     * The vms destroyed.
     */
    protected int vmsDestroyed;

    /**
     * The datacenter ids list.
     */
    protected List<Integer> datacenterIdsList;

    /**
     * The datacenter requested ids list.
     */
    protected List<Integer> datacenterRequestedIdsList;

    /**
     * The vms to datacenters map.
     */
    protected Map<Integer, Integer> vmsToDatacentersMap;

    /**
     * The datacenter characteristics list.
     */
    protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;

    /**
     * Created a new DatacenterBroker object.
     *
     * @param name name to be associated with this entity (as required by Sim_entity class from
     *             simjava package)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public DataCenterBroker1(String name) throws Exception {
        super(name);

        setVmList(new ArrayList<MyVm>());
        setVmsCreatedList(new ArrayList<MyVm>());
        setCloudletList(new ArrayList<CustomCloudlet>());
        setCloudletSubmittedList(new ArrayList<CustomCloudlet>());
        setCloudletReceivedList(new ArrayList<CustomCloudlet>());

        cloudletsSubmitted = 0;
        setVmsRequested(0);
        setVmsAcks(0);
        setVmsDestroyed(0);

        setDatacenterIdsList(new LinkedList<Integer>());
        setDatacenterRequestedIdsList(new ArrayList<Integer>());
        setVmsToDatacentersMap(new HashMap<Integer, Integer>());
        setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
    }

    /**
     * This method is used to send to the broker the list with virtual machines that must be
     * created.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    public void submitVmList(List<? extends MyVm> list) {
        getVmList().addAll(list);
    }

    /**
     * This method is used to send to the broker the list of cloudlets.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    public void submitCloudletList(List<? extends CustomCloudlet> list) {
        getCloudletList().addAll(list);
    }

    /**
     * Specifies that a given cloudlet must run in a specific virtual machine.
     *
     * @param cloudletId ID of the cloudlet being bount to a vm
     * @param vmId       the vm id
     * @pre cloudletId > 0
     * @pre id > 0
     * @post $none
     */
    public void bindCloudletToVm(int cloudletId, int vmId) {
        CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            // Resource characteristics answer
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                processResourceCharacteristics(ev);
                break;
            // VM Creation answer
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev);
                break;
            // A finished cloudlet returned
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
                break;
            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Process the return of a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            setDatacenterRequestedIdsList(new ArrayList<Integer>());
            createVmsInDatacenter(getDatacenterIdsList().get(0));
        }
    }

    /**
     * Process a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristicsRequest(SimEvent ev) {
        setDatacenterIdsList(CloudSim.getCloudResourceList());
        setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloud Resource List received with "
                + getDatacenterIdsList().size() + " resource(s)");

        for (Integer datacenterId : getDatacenterIdsList()) {
            sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
        }
    }

    /**
     * Process the ack received due to a request for VM creation.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
            Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
                    + " has been created in Datacenter #" + datacenterId + ", Host #"
                    + VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
        } else {
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
                    + " failed in Datacenter #" + datacenterId);
        }

        incrementVmsAcks();

        // all the requested VMs have been created
        if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
            submitCloudlets();
        } else {
            // all the acks received, but some VMs were not created
            if (getVmsRequested() == getVmsAcks()) {
                // find id of the next datacenter that has not been tried
                for (int nextDatacenterId : getDatacenterIdsList()) {
                    if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
                        createVmsInDatacenter(nextDatacenterId);
                        return;
                    }
                }

                // all datacenters already queried
                if (getVmsCreatedList().size() > 0) { // if some vm were created
                    submitCloudlets();
                } else { // no vms created. abort
                    Log.printLine(CloudSim.clock() + ": " + getName()
                            + ": none of the required VMs could be created. Aborting");
                    finishExecution();
                }
            }
        }
    }

    /**
     * Process a cloudlet return event.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processCloudletReturn(SimEvent ev) {
        CustomCloudlet cloudlet = (CustomCloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
                + " received");
        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
            Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // some cloudlets haven't finished yet
            if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
                // all the cloudlets sent finished. It means that some bount
                // cloudlet is waiting its VM be created
                clearDatacenters();
                createVmsInDatacenter(0);
            }

        }
    }


    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
            return;
        }

        Log.printLine(getName() + ".processOtherEvent(): "
                + "Error - event unknown by this DatacenterBroker.");
    }

    /**
     * Create the virtual machines in a datacenter.
     *
     * @param datacenterId Id of the chosen PowerDatacenter
     * @pre $none
     * @post $none
     */
    protected void createVmsInDatacenter(int datacenterId) {
        // send as much vms as possible for this datacenter before trying the next one
        int requestedVms = 0;
        String datacenterName = CloudSim.getEntityName(datacenterId);
        for (MyVm vm : getVmList()) {
            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
                        + " in " + datacenterName);
                sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                requestedVms++;
            }
        }

        getDatacenterRequestedIdsList().add(datacenterId);

        setVmsRequested(requestedVms);
        setVmsAcks(0);
    }

    /**
     * Submit cloudlets to the created VMs.
     *
     * @pre $none
     * @post $none
     */
    protected void submitCloudlets() {
        List<CustomCloudlet> specialCloudletList = new ArrayList<CustomCloudlet>();
        List<CustomCloudlet> normalCloudletList = new ArrayList<CustomCloudlet>();

        for (CustomCloudlet cloudlet : getCloudletList()) {
            if (cloudlet.getVmId() == -1 && cloudlet.getPriority() == Constant1.PRIORITY_SPECIAL) {
                specialCloudletList.add(cloudlet);
            } else if (cloudlet.getVmId() == -1 && cloudlet.getPriority() == Constant1.PRIORITY_NORMAL) {
                normalCloudletList.add(cloudlet);
            }
        }

        List<CustomCloudlet> tempCloudletList = new ArrayList<CustomCloudlet>();
        while (!specialCloudletList.isEmpty() || !normalCloudletList.isEmpty()) {
            if (!specialCloudletList.isEmpty()) {
                tempCloudletList.add(specialCloudletList.get(0));
                specialCloudletList.remove(0);
            }
            if (!specialCloudletList.isEmpty()) {
                tempCloudletList.add(specialCloudletList.get(0));
                specialCloudletList.remove(0);
            }
            if (!normalCloudletList.isEmpty()) {
                tempCloudletList.add(normalCloudletList.get(0));
                normalCloudletList.remove(0);
            }
        }
        tempCloudletList.sort(Comparator.comparingInt(o -> (int) o.getDelayCloudlet()));
        Collections.sort(tempCloudletList, new Comparator() {

            public int compare(Object o1, Object o2) {

                Double x1 = ((CustomCloudlet) o1).getDelayCloudlet();
                Double x2 = ((CustomCloudlet) o2).getDelayCloudlet();
                int sComp = x1.compareTo(x2);

                if (sComp != 0) {
                    return sComp;
                }

                Integer a1 = ((CustomCloudlet) o1).getPriority();
                Integer a2 = ((CustomCloudlet) o2).getPriority();
                return a2.compareTo(a1);
            }});

        ArrayList<MyVm> specialVmsList = new ArrayList<>();
        ArrayList<MyVm> normalVmsList = new ArrayList<>();

        for (int i = 0; i < Constant1.SPECIAL_MACHINES; i++) {
            specialVmsList.add(VmList.getById(getVmsCreatedList(), i));
        }

        for (int i = Constant1.SPECIAL_MACHINES; i < Constant1.MACHINES; i++) {
            normalVmsList.add(VmList.getById(getVmsCreatedList(), i));
        }

        specialVmsList.sort(Comparator.comparingInt(o -> o.cyclesCompleted));
        normalVmsList.sort(Comparator.comparingInt(o -> o.cyclesCompleted));

        for (CustomCloudlet cloudlet : tempCloudletList) {
            MyVm vm, vm1, vm2;
            // if user didn't bind this cloudlet and it has not been executed yet
            if (cloudlet.getVmId() == -1) {
                if (cloudlet.getPriority() == Constant1.PRIORITY_NORMAL) {
                    vm = VmList.getById(getVmsCreatedList(), normalVmsList.get(0).getId());
                    if (vm != null && vm.cyclesCompleted + cloudlet.getCloudletLength()/Constant1.LENGTH_FACTOR <= vm.maxCycles) {
                        vm.cyclesCompleted += cloudlet.getCloudletLength()/Constant1.LENGTH_FACTOR;
                        normalVmsList.sort(Comparator.comparingInt(o -> o.cyclesCompleted));
                        Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                                + cloudlet.getCloudletId() + " to VM #" + vm.getId());
                        cloudlet.setVmId(vm.getId());

                        double delayValue = cloudlet.getDelayCloudlet();

                        if (delayValue != 0) {
                            send(getVmsToDatacentersMap().get(vm.getId()), delayValue, CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        } else {
                            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        }

                        cloudletsSubmitted++;
                        getCloudletSubmittedList().add(cloudlet);
                        // remove submitted cloudlets from waiting list
                        for (CustomCloudlet cloudlet1 : getCloudletSubmittedList()) {
                            getCloudletList().remove(cloudlet1);
                            //       tempCloudletList.remove(cloudlet1);
                        }
                    }
                }
                else if (cloudlet.getPriority() == Constant1.PRIORITY_SPECIAL) {
                    vm1 = VmList.getById(getVmsCreatedList(), specialVmsList.get(0).getId());
                    vm2 = VmList.getById(getVmsCreatedList(), normalVmsList.get(0).getId());
                    if (vm1 != null && vm1.cyclesCompleted + cloudlet.getCloudletLength()/Constant1.LENGTH_FACTOR <= vm1.maxCycles) {
                        vm1.cyclesCompleted += cloudlet.getCloudletLength()/Constant1.LENGTH_FACTOR;
                        specialVmsList.sort(Comparator.comparingInt(o -> o.cyclesCompleted));
                        Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                                + cloudlet.getCloudletId() + " to VM #" + vm1.getId());
                        cloudlet.setVmId(vm1.getId());

                        double delayValue = cloudlet.getDelayCloudlet();

                        if (delayValue != 0) {
                            send(getVmsToDatacentersMap().get(vm1.getId()), delayValue, CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        } else {
                            sendNow(getVmsToDatacentersMap().get(vm1.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        }

                        cloudletsSubmitted++;
                        getCloudletSubmittedList().add(cloudlet);
                        // remove submitted cloudlets from waiting list
                        for (CustomCloudlet cloudlet1 : getCloudletSubmittedList()) {
                            getCloudletList().remove(cloudlet1);
                            //      tempCloudletList.remove(cloudlet1);
                        }
                    }
                    else if (vm2 != null && vm2.cyclesCompleted + cloudlet.getCloudletLength()/Constant1.LENGTH_FACTOR <= vm2.maxCycles) {
                        vm2.cyclesCompleted += cloudlet.getCloudletLength()/Constant1.LENGTH_FACTOR;
                        normalVmsList.sort(Comparator.comparingInt(o -> o.cyclesCompleted));
                        Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                                + cloudlet.getCloudletId() + " to VM #" + vm2.getId());
                        cloudlet.setVmId(vm2.getId());

                        double delayValue = cloudlet.getDelayCloudlet();

                        if (delayValue != 0) {
                            send(getVmsToDatacentersMap().get(vm2.getId()), delayValue, CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        } else {
                            sendNow(getVmsToDatacentersMap().get(vm2.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        }

                        cloudletsSubmitted++;
                        getCloudletSubmittedList().add(cloudlet);
                        // remove submitted cloudlets from waiting list
                        for (CustomCloudlet cloudlet1 : getCloudletSubmittedList()) {
                            getCloudletList().remove(cloudlet1);
                            //       tempCloudletList.remove(cloudlet1);
                        }
                    } else {
                        Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
                                + cloudlet.getCloudletId() + ": bount VM not available");
                    }
                }
                else { // submit to the specific vm
                    vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                    if (vm == null) { // vm was not created
                        Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
                                + cloudlet.getCloudletId() + ": bount VM not available");
                        continue;
                    }
                    vm.cyclesCompleted += cloudlet.getCloudletLength();
                    Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                            + cloudlet.getCloudletId() + " to VM #" + vm.getId());
                    cloudlet.setVmId(vm.getId());

                    double delayValue = cloudlet.getDelayCloudlet();

                    if (delayValue != 0) {
                        send(getVmsToDatacentersMap().get(vm.getId()), delayValue, CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                    } else {
                        sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                    }

                    cloudletsSubmitted++;
                    getCloudletSubmittedList().add(cloudlet);
                    // remove submitted cloudlets from waiting list
                    for (CustomCloudlet cloudlet1 : getCloudletSubmittedList()) {
                        getCloudletList().remove(cloudlet1);
                        //       tempCloudletList.remove(cloudlet1);
                    }
                }
            }
        }

        for (CustomCloudlet cloudlet : getCloudletSubmittedList()) {
            tempCloudletList.remove(cloudlet);
            getCloudletList().remove(cloudlet);
        }
    }

    /**
     * Destroy the virtual machines running in datacenters.
     *
     * @pre $none
     * @post $none
     */
    protected void clearDatacenters() {
        for (MyVm vm : getVmsCreatedList()) {
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Destroying VM #" + vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
        }

        getVmsCreatedList().clear();
    }

    /**
     * Send an internal event communicating the end of the simulation.
     *
     * @pre $none
     * @post $none
     */
    protected void finishExecution() {
        sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#startEntity()
     */
    @Override
    public void startEntity() {
        Log.printLine(getName() + " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends MyVm> List<T> getVmList() {
        return (List<T>) vmList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T>    the generic type
     * @param vmList the new vm list
     */
    protected <T extends MyVm> void setVmList(List<T> vmList) {
        this.vmList = vmList;
    }

    /**
     * Gets the cloudlet list.
     *
     * @param <T> the generic type
     * @return the cloudlet list
     */
    @SuppressWarnings("unchecked")
    public <T extends CustomCloudlet> List<T> getCloudletList() {
        return (List<T>) cloudletList;
    }

    /**
     * Sets the cloudlet list.
     *
     * @param <T>          the generic type
     * @param cloudletList the new cloudlet list
     */
    protected <T extends CustomCloudlet> void setCloudletList(List<T> cloudletList) {
        this.cloudletList = cloudletList;
    }

    /**
     * Gets the cloudlet submitted list.
     *
     * @param <T> the generic type
     * @return the cloudlet submitted list
     */
    @SuppressWarnings("unchecked")
    public <T extends CustomCloudlet> List<T> getCloudletSubmittedList() {
        return (List<T>) cloudletSubmittedList;
    }

    /**
     * Sets the cloudlet submitted list.
     *
     * @param <T>                   the generic type
     * @param cloudletSubmittedList the new cloudlet submitted list
     */
    protected <T extends CustomCloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
        this.cloudletSubmittedList = cloudletSubmittedList;
    }

    /**
     * Gets the cloudlet received list.
     *
     * @param <T> the generic type
     * @return the cloudlet received list
     */
    @SuppressWarnings("unchecked")
    public <T extends CustomCloudlet> List<T> getCloudletReceivedList() {
        return (List<T>) cloudletReceivedList;
    }

    /**
     * Sets the cloudlet received list.
     *
     * @param <T>                  the generic type
     * @param cloudletReceivedList the new cloudlet received list
     */
    protected <T extends CustomCloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
        this.cloudletReceivedList = cloudletReceivedList;
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends MyVm> List<T> getVmsCreatedList() {
        return (List<T>) vmsCreatedList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T>            the generic type
     * @param vmsCreatedList the vms created list
     */
    protected <T extends MyVm> void setVmsCreatedList(List<T> vmsCreatedList) {
        this.vmsCreatedList = vmsCreatedList;
    }

    /**
     * Gets the vms requested.
     *
     * @return the vms requested
     */
    protected int getVmsRequested() {
        return vmsRequested;
    }

    /**
     * Sets the vms requested.
     *
     * @param vmsRequested the new vms requested
     */
    protected void setVmsRequested(int vmsRequested) {
        this.vmsRequested = vmsRequested;
    }

    /**
     * Gets the vms acks.
     *
     * @return the vms acks
     */
    protected int getVmsAcks() {
        return vmsAcks;
    }

    /**
     * Sets the vms acks.
     *
     * @param vmsAcks the new vms acks
     */
    protected void setVmsAcks(int vmsAcks) {
        this.vmsAcks = vmsAcks;
    }

    /**
     * Increment vms acks.
     */
    protected void incrementVmsAcks() {
        vmsAcks++;
    }

    /**
     * Gets the vms destroyed.
     *
     * @return the vms destroyed
     */
    protected int getVmsDestroyed() {
        return vmsDestroyed;
    }

    /**
     * Sets the vms destroyed.
     *
     * @param vmsDestroyed the new vms destroyed
     */
    protected void setVmsDestroyed(int vmsDestroyed) {
        this.vmsDestroyed = vmsDestroyed;
    }

    /**
     * Gets the datacenter ids list.
     *
     * @return the datacenter ids list
     */
    protected List<Integer> getDatacenterIdsList() {
        return datacenterIdsList;
    }

    /**
     * Sets the datacenter ids list.
     *
     * @param datacenterIdsList the new datacenter ids list
     */
    protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
        this.datacenterIdsList = datacenterIdsList;
    }

    /**
     * Gets the vms to datacenters map.
     *
     * @return the vms to datacenters map
     */
    protected Map<Integer, Integer> getVmsToDatacentersMap() {
        return vmsToDatacentersMap;
    }

    /**
     * Sets the vms to datacenters map.
     *
     * @param vmsToDatacentersMap the vms to datacenters map
     */
    protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
        this.vmsToDatacentersMap = vmsToDatacentersMap;
    }

    /**
     * Gets the datacenter characteristics list.
     *
     * @return the datacenter characteristics list
     */
    protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
        return datacenterCharacteristicsList;
    }

    /**
     * Sets the datacenter characteristics list.
     *
     * @param datacenterCharacteristicsList the datacenter characteristics list
     */
    protected void setDatacenterCharacteristicsList(
            Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
        this.datacenterCharacteristicsList = datacenterCharacteristicsList;
    }

    /**
     * Gets the datacenter requested ids list.
     *
     * @return the datacenter requested ids list
     */
    protected List<Integer> getDatacenterRequestedIdsList() {
        return datacenterRequestedIdsList;
    }

    /**
     * Sets the datacenter requested ids list.
     *
     * @param datacenterRequestedIdsList the new datacenter requested ids list
     */
    protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
        this.datacenterRequestedIdsList = datacenterRequestedIdsList;
    }

}
