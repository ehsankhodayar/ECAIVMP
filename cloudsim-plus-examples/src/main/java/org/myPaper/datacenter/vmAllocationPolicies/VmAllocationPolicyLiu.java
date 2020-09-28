package org.myPaper.datacenter.vmAllocationPolicies;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import org.myPaper.acsAlgorithms.Liu.Liu;
import org.myPaper.datacenter.DatacenterPro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class VmAllocationPolicyLiu
    extends VmAllocationPolicyAbstract implements VmAllocationPolicyAcsBased {
    protected static final Logger LOGGER = LoggerFactory.getLogger(VmAllocationPolicyLiu.class.getSimpleName());

    private final Liu LIU_ACS;

    public VmAllocationPolicyLiu(Liu liu) {
        LIU_ACS = liu;
    }

    @Override
    protected Optional<Host> defaultFindHostForVm(Vm vm) {
        if (vm.getHost() != Host.NULL) {
            vm.getHost().destroyTemporaryVm(vm);
            if (vm.getHost().isSuitableForVm(vm)) {
                return Optional.of(vm.getHost());
            } else {
                vm.setHost(Host.NULL);
                return Optional.of(Host.NULL);
            }
        }

        return findSuitableHostForVm(vm);
    }

    public Optional<Host> findSuitableHostForVm(final Vm vm) {
        List<Vm> vmList = new ArrayList<>();
        vmList.add(vm);

        Optional<Map<Vm, Host>> solution = findSolutionForVms(vmList, getHostList());

        return solution.map(vmHostMap -> vmHostMap.get(vm));
    }

    @Override
    public Optional<Map<Vm, Host>> findSolutionForVms(final List<Vm> vmList, List<Host> allowedHostList) {
        if (vmList.isEmpty()) {
            return Optional.empty();
        }

        Optional<Map<Vm, Host>> solution = LIU_ACS.getBestSolution(vmList, getDatacenter(), allowedHostList);

        if (solution.isPresent() && !solution.get().isEmpty()) {
            LOGGER.info("{}: {} found a solution for the requested Vm list.",
                getDatacenter().getSimulation().clockStr(),
                getDatacenter());
        }else {
            LOGGER.warn("{}: {} could not find any solution for the requested Vm list!",
                getDatacenter().getSimulation().clockStr(),
                getDatacenter());
        }

        return solution;
    }

    /**
     * Gets the migration map of the given solution
     *
     * @param solution the target solution
     * @return the migration map
     */
    public Map<Vm, Host> getSolutionMigrationMap(final Map<Vm, Host> solution) {
        return solution.entrySet().parallelStream()
            .filter(vmHostEntry -> vmHostEntry.getKey().isCreated())
            .filter(vmHostEntry -> vmHostEntry.getKey().getHost() != vmHostEntry.getValue())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets the solution without migration map.
     *
     * @param solution the target solution
     * @return the solution
     */
    public Map<Vm, Host> getSolutionWithoutMigrationMap(final Map<Vm, Host> solution) {
        return solution.entrySet().parallelStream()
            .filter(vmHostEntry -> !vmHostEntry.getKey().isCreated())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
