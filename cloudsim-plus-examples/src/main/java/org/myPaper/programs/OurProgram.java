package org.myPaper.programs;

import org.cloudbus.cloudsim.core.CloudSim;
import org.myPaper.broker.DatacenterBrokeFFD;
import org.myPaper.broker.DatacenterBrokerOurAlgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class OurProgram extends ParentClass{
    public OurProgram(final String directory, final int workloadsNumber) {
        super(directory, false, workloadsNumber);

        runProgram();
    }

    private void runProgram() {
        simulation = new CloudSim();

        //Brokers of Providers
        broker1 =
            new DatacenterBrokerOurAlgo(simulation, "Cloud-Broker-Provider1", Arrays.asList(datacenter1, datacenter2, datacenter3, datacenter4));
        broker1.setVmDestructionDelay(VM_DESTRUCTION_DELAY);
        broker1.setRetryFailedVms(false);

        //Provider1 Dataceneters
        datacenter1 = createDatacenter("PaloAlto-California-USA", -7,
            DC1_OFF_SITE_ENERGY_PRICE, DC1_CARBON_FOOTPRINT_RATE, DC1_CARBON_TAX, DC1_WEATHER_DATASET);
        datacenter2 = createDatacenter("Richmond-Virginia-USA", -4,
            DC2_OFF_SITE_ENERGY_PRICE, DC2_CARBON_FOOTPRINT_RATE, DC2_CARBON_TAX, DC2_WEATHER_DATASET);
        datacenter3 = createDatacenter("Tokyo-Japan", +9,
            DC3_OFF_SITE_ENERGY_PRICE, DC3_CARBON_FOOTPRINT_RATE, DC3_CARBON_TAX, DC3_WEATHER_DATASET);
        datacenter4 = createDatacenter("Sydney-Australia", +10,
            DC4_OFF_SITE_ENERGY_PRICE, DC4_CARBON_FOOTPRINT_RATE, DC4_CARBON_TAX, DC4_WEATHER_DATASET);

        //Cloudlets and VMs
        cloudletList = createCloudlets();
        createVms(cloudletList);

        //Simulation
        simulation.addOnClockTickListener(this::simulationClocktickListener);
        simulation.terminateAt(SIMULATION_TIME);
        simulation.start();

        generateExperimentalResults();
    }
}
