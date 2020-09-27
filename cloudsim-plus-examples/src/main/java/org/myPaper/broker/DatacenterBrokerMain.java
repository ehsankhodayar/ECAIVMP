package org.myPaper.broker;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.CloudSimEvent;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;
import org.myPaper.datacenter.DatacenterPro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DatacenterBrokerMain extends DatacenterBrokerAbstractCustomized {
    public static final DatacenterBrokerMain NULL = null;

    /**
     * Creates a DatacenterBroker giving a specific name.
     * Subclasses usually should provide this constructor and
     * and overloaded version that just requires the {@link CloudSim} parameter.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name       the DatacenterBroker name
     * @param datacenterList list of connected datacenters to this broker
     */
    public DatacenterBrokerMain(CloudSim simulation, String name, List<Datacenter> datacenterList) {
        super(simulation, name);

        Collections.shuffle(datacenterList);
    }

    @Override
    protected Datacenter defaultDatacenterMapper(Datacenter lastDatacenter, Vm vm) {
        return null;
    }

    @Override
    protected Vm defaultVmMapper(Cloudlet cloudlet) {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }else {
            return Vm.NULL;
        }
    }

    /**
     * Gets the datacenterPro
     * @param datacenter datacenter object
     * @return datacenterPro
     */
    protected DatacenterPro getDatacenterPro(Datacenter datacenter) {
        return (DatacenterPro) datacenter;
    }

    /**
     * Fails the given list of Vms
     *
     * @param vmList the failed Vm list
     */
    protected void failVms(final List<Vm> vmList) {
        List<Vm> waitingVmList = new ArrayList<>(vmList);
        waitingVmList.forEach(vm -> {
            SimEvent simEvent = new CloudSimEvent(this, CloudSimTags.VM_CREATE_ACK, vm);
            processEvent(simEvent);
        });
    }

    @Override
    public <T extends Vm> List<T> getVmWaitingList() {
        return (List<T>) vmWaitingList.parallelStream()
            .filter(vm -> !getVmWaitingAckList().contains(vm))
            .collect(Collectors.toList());
    }
}
