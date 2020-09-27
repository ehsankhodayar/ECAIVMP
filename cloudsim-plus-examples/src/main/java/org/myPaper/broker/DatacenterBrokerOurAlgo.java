package org.myPaper.broker;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.stream.Collectors;

public class DatacenterBrokerOurAlgo extends DatacenterBrokerMain {
    /**
     * Creates a DatacenterBroker giving a specific name.
     * Subclasses usually should provide this constructor and
     * and overloaded version that just requires the {@link CloudSim} parameter.
     *
     * @param simulation     the CloudSim instance that represents the simulation the Entity is related to
     * @param name           the DatacenterBroker name
     * @param datacenterList list of connected datacenters to this broker
     */
    public DatacenterBrokerOurAlgo(CloudSim simulation, String name, List<Datacenter> datacenterList) {
        super(simulation, name, datacenterList);
    }

    @Override
    protected Datacenter defaultDatacenterMapper(Datacenter lastDatacenter, Vm vm) {
        if (getDatacenterList().isEmpty()) {
            throw new IllegalStateException("You don't have any Datacenter created.");
        }

        //Look for suitable resources inside the datacenters of current cloud provider
        LOGGER.info("{}: {} is trying to find suitable resources for allocating to the new Vm creation requests inside the available datacenters.",
            getSimulation().clockStr(),
            this);

        List<Host> suggestedHostList = new ArrayList<>();
        getDatacenterList().forEach(datacenter -> {
            Optional<Host> suitableHost = getDatacenterSuitableHost(datacenter, vm);

            suitableHost.ifPresent(suggestedHostList::add);
        });

        if (suggestedHostList.isEmpty()) {
            LOGGER.warn("{}: {} could not find suitable resource for all the new Vm creation requests inside the available datacenters!",
                getSimulation().clockStr(),
                this);

            return Datacenter.NULL;
        }

        Host selectedHost = chooseBestVmpSuggestion(vm, suggestedHostList);

        if (selectedHost.isSuitableForVm(vm)) {
            selectedHost.createTemporaryVm(vm);
            vm.setHost(selectedHost);

            return selectedHost.getDatacenter();
        }else {
            throw new IllegalStateException("The selected host is not suitable for the requested VM!");
        }
    }

    @Override
    protected boolean requestDatacenterToCreateWaitingVms(final boolean isFallbackDatacenter) {
        if (getVmWaitingList().isEmpty()) {
            return true;
        }

        List<Vm> failedVmList = new ArrayList<>();

        for (Vm vm : getVmWaitingList()) {
            this.lastSelectedDc = defaultDatacenterMapper(lastSelectedDc, vm);

            if (lastSelectedDc != Datacenter.NULL) {
                this.vmCreationRequests += requestVmCreation(lastSelectedDc, isFallbackDatacenter, vm);
            }else {
                failedVmList.add(vm);
            }
        }

        if (!failedVmList.isEmpty()) {
            failVms(failedVmList);
        }

        return failedVmList.isEmpty();
    }

    private Optional<Host> getDatacenterSuitableHost(final Datacenter datacenter, final Vm vm) {
        final List<Host> suitableHostList = datacenter.getHostList().parallelStream()
            .filter(host -> host.isSuitableForVm(vm))
            .collect(Collectors.toList());

        if (suitableHostList.isEmpty()) {
            return Optional.empty();
        }

        Map<Host, Double> hostPreferenceMap = suitableHostList.parallelStream()
            .collect(Collectors.toMap(host -> host, host -> (1/getIncreaseInPowerConsumption(host, vm)) * (host.getVmList().size() +1)));

        Optional<Map.Entry<Host, Double>> suitableHost =
            hostPreferenceMap.entrySet().parallelStream().max(Comparator.comparingDouble(Map.Entry::getValue));

        return suitableHost.map(Map.Entry::getKey);
    }

    private Host chooseBestVmpSuggestion(final Vm vm, List<Host> suggestedHostList) {
        if (vm == Vm.NULL) {
            throw new IllegalStateException("Vm could not be null!");
        }

        if (suggestedHostList.isEmpty()) {
            throw new IllegalStateException("The suggested host list could not be empty!");
        }

        Map<Host, Double> hostIncreaseInPowerConsumptionMap = new HashMap<>();
        Map<Host, Double> hostIncreaseInCarbonEmissionMap = new HashMap<>();

        for (Host host : suggestedHostList) {
            //Energy consumption
            double increaseInITPowerConsumption = getIncreaseInPowerConsumption(host, vm);
            double increaseInOverhead =
                increaseInITPowerConsumption * (getDatacenterPro(host.getDatacenter()).getDatacenterDynamicPUE(increaseInITPowerConsumption) - 1);
            double totalPowerConsumption = increaseInITPowerConsumption + increaseInOverhead;
            hostIncreaseInPowerConsumptionMap.put(host, totalPowerConsumption);

            //Carbon emission
            double carbonEmission = getDatacenterPro(host.getDatacenter()).getTotalCarbonFootprint(totalPowerConsumption / 3600);
            hostIncreaseInCarbonEmissionMap.put(host, carbonEmission);
        }

        List<Host> hostsWithMinimumIncreaseInPowerConsumption = new ArrayList<>();

        double minimumIncreaseInPowerConsumption =
            Collections.min(hostIncreaseInPowerConsumptionMap.entrySet(), Map.Entry.comparingByValue()).getValue();

        hostIncreaseInPowerConsumptionMap.forEach((host, powerConsumption) -> {
            if (powerConsumption <= minimumIncreaseInPowerConsumption) {
                hostsWithMinimumIncreaseInPowerConsumption.add(host);
            }
        });

        if (hostsWithMinimumIncreaseInPowerConsumption.size() > 1) {
            return Collections.min(hostIncreaseInCarbonEmissionMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        }else {
            return hostsWithMinimumIncreaseInPowerConsumption.get(0);
        }
    }

    private double getIncreaseInPowerConsumption(final Host host, final Vm newVm) {
        if (!host.isSuitableForVm(newVm)) {
            throw new IllegalStateException("The request VM is not suitable for the given host!");
        }

        final double hostNewCpuUtilization =
            (host.getCpuMipsUtilization() + newVm.getTotalMipsCapacity()) / host.getTotalMipsCapacity();
        final double hostCurrentPowerConsumption = host.isActive() ? host.getPowerModel().getPower() : 10;

        return host.getPowerModel().getPower(hostNewCpuUtilization) - hostCurrentPowerConsumption;
    }
}
