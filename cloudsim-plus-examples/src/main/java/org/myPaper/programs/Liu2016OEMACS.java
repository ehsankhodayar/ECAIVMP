package org.myPaper.programs;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.myPaper.acsAlgorithms.Liu.Liu;
import org.myPaper.acsAlgorithms.Liu.Liu2016;
import org.myPaper.broker.DatacenterBrokerLiu2016OEMACS;
import org.myPaper.datacenter.vmAllocationPolicies.VmAllocationPolicyLiu;

import java.util.Arrays;

public class Liu2016OEMACS extends ParentClass {

    public Liu2016OEMACS(final String directory, final int workloadsNumber) {
        super(directory, false, workloadsNumber);

        runProgram();
    }

    private void runProgram() {
        simulation = new CloudSim();

        //Brokers of Providers
        broker1 = new DatacenterBrokerLiu2016OEMACS(simulation, "Cloud-Broker-Provider1", Arrays.asList(datacenter1, datacenter2, datacenter3, datacenter4));
        broker1.setVmDestructionDelay(VM_DESTRUCTION_DELAY);
        broker1.setRetryFailedVms(false);

        //Provider1 Dataceneters
        datacenter1 = createDatacenter("PaloAlto-California-USA", -7,
            DC1_OFF_SITE_ENERGY_PRICE, DC1_CARBON_FOOTPRINT_RATE, DC1_CARBON_TAX, DC1_WEATHER_DATASET);
        datacenter1.setVmAllocationPolicy(createNewVmAllocationPolicy());

        datacenter2 = createDatacenter("Richmond-Virginia-USA", -4,
            DC2_OFF_SITE_ENERGY_PRICE, DC2_CARBON_FOOTPRINT_RATE, DC2_CARBON_TAX, DC2_WEATHER_DATASET);
        datacenter2.setVmAllocationPolicy(createNewVmAllocationPolicy());

        datacenter3 = createDatacenter("Tokyo-Japan", +9,
            DC3_OFF_SITE_ENERGY_PRICE, DC3_CARBON_FOOTPRINT_RATE, DC3_CARBON_TAX, DC3_WEATHER_DATASET);
        datacenter3.setVmAllocationPolicy(createNewVmAllocationPolicy());

        datacenter4 = createDatacenter("Sydney-Australia", +10,
            DC4_OFF_SITE_ENERGY_PRICE, DC4_CARBON_FOOTPRINT_RATE, DC4_CARBON_TAX, DC4_WEATHER_DATASET);
        datacenter4.setVmAllocationPolicy(createNewVmAllocationPolicy());

        //Cloudlets and VMs
        cloudletList = createCloudlets();
        createVms(cloudletList);

        //Simulation
        simulation.addOnClockTickListener(this::simulationClocktickListener);
        simulation.terminateAt(SIMULATION_TIME);
        simulation.start();

        generateExperimentalResults();
    }

    /**
     * Creates a new VM allocation policy according to the OurAcs 2016 paper.
     *
     * @return a new VM allocation migration policy
     */
    private VmAllocationPolicy createNewVmAllocationPolicy() {
        Liu liu = new Liu2016(2, 3, 0.7, 0.1, 0.1, 2);
        return new VmAllocationPolicyLiu(liu);
    }
}
