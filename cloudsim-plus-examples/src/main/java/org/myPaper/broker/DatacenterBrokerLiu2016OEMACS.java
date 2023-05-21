package org.myPaper.broker;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.vms.Vm;
import org.myPaper.datacenter.DatacenterPro;
import org.myPaper.datacenter.vmAllocationPolicies.VmAllocationPolicyLiu;
import org.myPaper.programs.ParentClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DatacenterBrokerLiu2016OEMACS extends DatacenterBrokerMain {

    /**
     * Creates a DatacenterBroker giving a specific name.
     * Subclasses usually should provide this constructor and
     * and overloaded version that just requires the {@link CloudSim} parameter.
     *
     * @param simulation            the CloudSim instance that represents the simulation the Entity is related to
     * @param name                  the DatacenterBroker name
     * @param datacenterList        list of connected datacenters to this broker
     */
    public DatacenterBrokerLiu2016OEMACS(CloudSim simulation,
                                         String name,
                                         List<Datacenter> datacenterList) {
        super(simulation, name, datacenterList);
    }

    @Override
    protected Vm defaultVmMapper(Cloudlet cloudlet) {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }

        return Vm.NULL;
    }

    @Override
    protected boolean requestDatacenterToCreateWaitingVms(final boolean isFallbackDatacenter) {
        if (getVmWaitingList().isEmpty()) {
            return true;
        }

        if (getDatacenterList().isEmpty()) {
            throw new IllegalStateException("You don't have any Datacenter created.");
        }

        double startTime = System.currentTimeMillis();

        boolean vmPlacementResult = runVmAllocationPolicy(isFallbackDatacenter);

        double finishTime = System.currentTimeMillis();
        double runTime= finishTime - startTime;
        ParentClass.executionTimeList.add(runTime);

        return vmPlacementResult;
    }

    /**
     * Runs the Vm allocation policy to find suitable hosts for the given Vm list at their requested data centers.
     *
     * @param isFallbackDatacenter set true if the fallback data center is true, false otherwise
     * @return true if the last selected data center is not Null, false otherwise
     */
    protected boolean runVmAllocationPolicy(boolean isFallbackDatacenter) {
        if (getVmWaitingList().isEmpty()) {
            return true;
        }

        if (getDatacenterList().isEmpty()) {
            throw new IllegalStateException("You don't have any Datacenter created.");
        }

        Map<Vm, Host> solutionMap;

        LOGGER.info("{}: {} is trying to find suitable resources for allocating to the new Vm creation requests inside the available datacenters.",
            getSimulation().clockStr(),
            getName());

        for (Datacenter datacenter : getDatacenterList()) {
            List<Host> allowedHostList = datacenter.getHostList();

            if (allowedHostList.isEmpty()) {
                continue;
            }

            VmAllocationPolicyLiu vmAllocationPolicy =
                (VmAllocationPolicyLiu) datacenter.getVmAllocationPolicy();

            solutionMap = vmAllocationPolicy.findSolutionForVms(getVmWaitingList(), datacenter.getHostList()).orElse(Collections.emptyMap());

            if (!solutionMap.isEmpty()) {
                LOGGER.info("{}: {} has found some suitable resources for allocating to the new Vm creation " +
                        "requests inside the available datacenters.",
                    getSimulation().clockStr(),
                    getName());

                sendVmCreationRequest(datacenter, solutionMap, isFallbackDatacenter);

                return true;
            }
        }

        LOGGER.warn("{}: {} could not find any suitable resource for allocating to " +
                "the new Vm creation requests inside the available datacenters!",
            getSimulation().clockStr(),
            getName());

        failVms(getVmWaitingList());

        return false;
    }

    private void sendVmCreationRequest(final Datacenter datacenter, final Map<Vm, Host> solutionMap, final boolean isFallbackDatacenter) {
        VmAllocationPolicyLiu vmAllocationPolicy =
            (VmAllocationPolicyLiu) datacenter.getVmAllocationPolicy();

        Map<Vm, Host> migrationMap = vmAllocationPolicy.getSolutionMigrationMap(solutionMap);
        for (Map.Entry<Vm, Host> vmHostEntry : vmAllocationPolicy.getSolutionWithoutMigrationMap(solutionMap).entrySet()) {
            Vm vm = vmHostEntry.getKey();
            Host host = vmHostEntry.getValue();
            double submissionDelay = 0;

            if (!migrationMap.isEmpty()) {
                //Considering the migration delay if the a not created VM has a same host with some VMs in the migrationMap
                for (Map.Entry<Vm, Host> migrationEntry : migrationMap.entrySet()) {
                    Vm vmInMigration = migrationEntry.getKey();
                    Host destinationHost = migrationEntry.getValue();

                    if (vmInMigration.getHost() == host) {
                        double migrationTime = vmInMigration.getRam().getCapacity() /
                            Conversion.bitesToBytes(destinationHost.getBw().getCapacity() * datacenter.getBandwidthPercentForMigration());
                        submissionDelay += (migrationTime + 1);
                    }
                }
            }

            if (host.isSuitableForVm(vm)) {
                host.createTemporaryVm(vm);
            }
            vm.setHost(vmHostEntry.getValue());
            vm.setSubmissionDelay(submissionDelay);
            this.vmCreationRequests += requestVmCreation(datacenter, isFallbackDatacenter, vmHostEntry.getKey());
        }

        if (!migrationMap.isEmpty()) {
            migrationMap.forEach((sourceVm, targetHost) -> {
                DatacenterPro datacenterPro = (DatacenterPro) sourceVm.getHost().getDatacenter();
                datacenterPro.requestVmMigration(sourceVm, targetHost);
            });
        }
    }
}
