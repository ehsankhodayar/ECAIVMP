package org.myPaper.broker;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import org.myPaper.programs.ParentClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatacenterBrokeFFD extends DatacenterBrokerMain {

    /**
     * Creates a DatacenterBroker giving a specific name.
     * Subclasses usually should provide this constructor and
     * and overloaded version that just requires the {@link CloudSim} parameter.
     *
     * @param simulation     the CloudSim instance that represents the simulation the Entity is related to
     * @param name           the DatacenterBroker name
     * @param datacenterList list of connected datacenters to this broker
     */
    public DatacenterBrokeFFD(CloudSim simulation, String name, List<Datacenter> datacenterList) {
        super(simulation, name, datacenterList);
    }

    @Override
    protected boolean requestDatacenterToCreateWaitingVms(final boolean isFallbackDatacenter) {
        if (getVmWaitingList().isEmpty()) {
            return true;
        }

        if (getDatacenterList().isEmpty()) {
            throw new IllegalStateException("You don't have any Datacenter created.");
        }

        //Look for suitable resources inside the datacenters of current cloud provider
        LOGGER.info("{}: {} is trying to find suitable resources for allocating to the new Vm creation requests inside the available datacenters.",
            getSimulation().clockStr(),
            getName());

        double startTime = System.currentTimeMillis();
        List<Vm> vmList = new ArrayList<>(getVmWaitingList());

        vmLoop:
        for (Vm vm : getVmWaitingList()) {
            for (Datacenter datacenter : getDatacenterList()) {
                List<Host> hostList = new ArrayList<>(datacenter.getHostList());
                Collections.shuffle(hostList);

                for (Host host : hostList) {
                    if (host.isSuitableForVm(vm)) {
                        vm.setHost(host);
                        host.createTemporaryVm(vm);
                        vmList.remove(vm);
                        this.vmCreationRequests += requestVmCreation(datacenter, isFallbackDatacenter, vm);

                        continue vmLoop;
                    }
                }
            }
        }

        double finishTime = System.currentTimeMillis();
        double runTime= finishTime - startTime;
        ParentClass.executionTimeList.add(runTime);

        if (vmList.isEmpty()) {
            LOGGER.info("{}: {} has found suitable resources for all the new Vm creation requests inside the available datacenters.",
                getSimulation().clockStr(),
                getName());

            return true;
        } else {
            LOGGER.warn("{}: {} could not find suitable resources for all the new Vm creation requests inside the available datacenters!",
                getSimulation().clockStr(),
                getName());

            failVms(vmList);

            return false;
        }
    }
}
