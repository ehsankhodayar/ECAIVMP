package org.myPaper.programs;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.util.SwfWorkloadFileReader;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.EventInfo;
import org.myPaper.additionalClasses.ExperimentalResults;
import org.myPaper.additionalClasses.UtilizationModelDynamicCustomized;
import org.myPaper.broker.DatacenterBrokerMain;
import org.myPaper.datacenter.DatacenterPowerSupplyOverheadPowerAware;
import org.myPaper.datacenter.DatacenterPro;
import org.myPaper.datacenter.vmAllocationPolicies.VmAllocationPolicyFirstFitCustomized;
import org.myPaper.host.instances.*;
import org.myPaper.vm.instances.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BiFunction;

public abstract class ParentClass {
    //SimulatorConfigurations
    protected static final int SIMULATION_TIME = 432000; //5 days
//    protected static final LocalTime SIMULATION_START_TIME = LocalTime.now();
    protected static final LocalDateTime SIMULATION_START_TIME = LocalDateTime.now();
    protected static CloudSim simulation;
    protected final String OUTPUT_DIRECTORY;

    //Broker Configurations
    protected static DatacenterBrokerMain broker1;

    //Datacenter Configurations
    protected final double SCHEDULING_INTERVAL = 100; //Every 100 seconds.
    protected final boolean LIVE_VM_MIGRATION;
    protected final double OVERLOAD_AND_UNDERLOAD_MONITORING_INTERVAL = 600; //Every 10 minutes.
    protected final double UNDERUTILIZATION_THRESHOLD = 0.3;
    protected final double OVERUTILIZATION_THRESHOLD = 0.9;
    //------------------------------------Datacenter1------------------------------------
    protected static DatacenterPro datacenter1; //Palo Alto, California, USA
    protected final String DC1_WEATHER_DATASET = "weather-info/Palo-Alto-California-US-weather-info-hourly-data.csv";
    protected final double DC1_OFF_SITE_ENERGY_PRICE = 12.37; //Cents/KWh
    protected final double DC1_CARBON_FOOTPRINT_RATE = 0.2060; //Tons/MWh
    protected final double DC1_CARBON_TAX = 15.30 * 100; //Cents/Ton
    //------------------------------------Datacenter2------------------------------------
    protected static DatacenterPro datacenter2; //Richmond, Virginia, USA
    protected final String DC2_WEATHER_DATASET = "weather-info/Richmond-Virginia-US-weather-info-hourly-data.csv";
    protected final double DC2_OFF_SITE_ENERGY_PRICE = 6.59; //Cents/KWh
    protected final double DC2_CARBON_FOOTPRINT_RATE = 0.3715; //Tons/MWh
    protected final double DC2_CARBON_TAX = 10.77 * 100; //Cents/Ton
    //------------------------------------Datacenter3------------------------------------
    protected static DatacenterPro datacenter3; //Tokyo, Japan
    protected final String DC3_WEATHER_DATASET = "weather-info/Tokyo-Japan-weather-info-hourly-data.csv";
    protected final double DC3_OFF_SITE_ENERGY_PRICE = 21.5; //Cents/KWh
    protected final double DC3_CARBON_FOOTPRINT_RATE = 0.4916; //Tons/MWh
    protected final double DC3_CARBON_TAX = 5.58 * 100; //Cents/Ton
    //------------------------------------Datacenter4------------------------------------
    protected static DatacenterPro datacenter4; //Sydney, Australia
    protected final String DC4_WEATHER_DATASET = "weather-info/Sydney-Australia-weather-info-hourly-data.csv";
    protected final double DC4_OFF_SITE_ENERGY_PRICE = 18.5; //Cents/KWh
    protected final double DC4_CARBON_FOOTPRINT_RATE = 0.82; //Tons/MWh
    protected final double DC4_CARBON_TAX = 10.20; //Dollar/Ton

    //Host Configurations
    protected final int MAXIMUM_NUMBER_OF_HOSTS = 180; //The maximum number of hosts must be equal or greater than the number of host categories
    protected final int IDLE_SHUTDOWN_DEADLINE = 150; //10 minutes

    //VM Configurations
    protected final double VM_DESTRUCTION_DELAY = 60; //one minute
    protected final double VM_SUBMISSION_INTERVAL = 100; //every 10 minutes
    protected static double lastVmListSubmissionTime;

    //Cloudlet Configurations
    protected static List<Cloudlet> cloudletList;
    protected final String SWF_WORKLOAD_DIRECTORY = "workload/swf/METACENTRUM_Sublist.swf";
    protected final int UTILIZATION_UPDATE_SCHEDULING_INTERVAL = 300; //5 minutes
    protected final int CLOUDLET_LENGTH = 50_000_000; //Million Instructions (MI)
    protected final int MAXIMUM_NUMBER_OF_CLOUDLETS; //cloudlets will be submitted dynamically to the broker during the simulation time

    public ParentClass(final String directory, final boolean liveVmMigration, final int totalVmReqs) {
        if (directory == null || !Files.exists(Paths.get(directory))) {
            throw new IllegalStateException("The given directory is not allowed!");
        }

        OUTPUT_DIRECTORY = directory;
        LIVE_VM_MIGRATION = liveVmMigration;
        MAXIMUM_NUMBER_OF_CLOUDLETS = totalVmReqs;
    }

    /**
     * Creates a Datacenter and its Hosts.
     * Each data center consists 300 hosts from 5 different instances.
     */
    protected DatacenterPro createDatacenter(final String name,
                                             final double timezone,
                                             final double energyPrice,
                                             final double carbonFootprintRate,
                                             final double carbonTax,
                                             final String weatherDataset) {
        final List<Host> hostList = new ArrayList<>();
        final int numberOfHostsFromEachInstance = (int) Math.floor((double) MAXIMUM_NUMBER_OF_HOSTS / 6);

        // Configuration 1
        for (int i = 0; i < numberOfHostsFromEachInstance; i++) {
            Host_Instance1_medium hostInstance = new Host_Instance1_medium();
            hostList.add(hostInstance.getHost().setIdleShutdownDeadline(IDLE_SHUTDOWN_DEADLINE));
        }

        // Configuration 2
        for (int i = 0; i < numberOfHostsFromEachInstance; i++) {
            Host_Instance2_large hostInstance = new Host_Instance2_large();
            hostList.add(hostInstance.getHost().setIdleShutdownDeadline(IDLE_SHUTDOWN_DEADLINE));
        }

        // Configuration 3
        for (int i = 0; i < numberOfHostsFromEachInstance; i++) {
            Host_Instance3_xlarge hostInstance = new Host_Instance3_xlarge();
            hostList.add(hostInstance.getHost().setIdleShutdownDeadline(IDLE_SHUTDOWN_DEADLINE));
        }

        // Configuration 4
        for (int i = 0; i < numberOfHostsFromEachInstance; i++) {
            Host_Instance4_2xlarge hostInstance = new Host_Instance4_2xlarge();
            hostList.add(hostInstance.getHost().setIdleShutdownDeadline(IDLE_SHUTDOWN_DEADLINE));
        }

        // Configuration 5
        for (int i = 0; i < numberOfHostsFromEachInstance; i++) {
            Host_Instance5_4xlarge hostInstance = new Host_Instance5_4xlarge();
            hostList.add(hostInstance.getHost().setIdleShutdownDeadline(IDLE_SHUTDOWN_DEADLINE));
        }

        // Configuration 6
        for (int i = 0; i < numberOfHostsFromEachInstance; i++) {
            Host_Instance6_8xLarge hostInstance = new Host_Instance6_8xLarge();
            hostList.add(hostInstance.getHost().setIdleShutdownDeadline(IDLE_SHUTDOWN_DEADLINE));
        }

        if (hostList.isEmpty()) {
            throw new IllegalStateException("The host list of a data center could not be empty!");
        }

        //Shuffling the list of hosts
        Collections.shuffle(hostList);

        DatacenterPro datacenterPro = new DatacenterPro(simulation, hostList, new VmAllocationPolicyFirstFitCustomized(), broker1);
        datacenterPro.setName(name);
        datacenterPro.setTimeZone(timezone);
        datacenterPro.setSchedulingInterval(SCHEDULING_INTERVAL);
        datacenterPro.setEnergyPriceModel(energyPrice);
        datacenterPro.setCarbonTaxAndFootprintRateModel(carbonTax, carbonFootprintRate);
        datacenterPro.setHostSearchRetryDelay(OVERLOAD_AND_UNDERLOAD_MONITORING_INTERVAL);
        datacenterPro.setPowerSupply(new DatacenterPowerSupplyOverheadPowerAware(datacenterPro));
        datacenterPro.enableHostOverUtilizedHistoryRecorder(true);
        datacenterPro.enableSaveHostAverageCpuUtilization(true);
        if (!LIVE_VM_MIGRATION) {
            datacenterPro.disableMigrations();
        }

        //Loads data center outside temperature dataset.
        try {
            datacenterPro.loadWeatherDataset(weatherDataset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenterPro;
    }

    /**
     * Creates a list of Cloudlets.
     */
    protected List<Cloudlet> createCloudlets() {
        SwfWorkloadFileReader reader = SwfWorkloadFileReader.getInstance(SWF_WORKLOAD_DIRECTORY, 2500);
        reader.setMaxLinesToRead(MAXIMUM_NUMBER_OF_CLOUDLETS);
        List<Cloudlet> cloudletList = reader.generateWorkload();
        cloudletList.forEach(cloudlet -> {
            cloudlet.setUtilizationModelRam(createDynamicUtilizationModel());
            cloudlet.setUtilizationModelBw(new UtilizationModelFull());
            cloudlet.setUtilizationModelCpu(createDynamicUtilizationModel());
            cloudlet.setLength(CLOUDLET_LENGTH);
        });

        if (cloudletList.isEmpty()) {
            throw new IllegalStateException("The cloudlet list of a data center could not be empty!");
        }

        return cloudletList;
    }

    /**
     * Creates a dynamic UtilizationModel for a resource instance
     * which will always use a random percentage as the utilization percentage and also will be change randomly
     * according to uniform distribution between 0-1.
     *
     * @return a dynamic utilization model
     */
    protected UtilizationModelDynamicCustomized createDynamicUtilizationModel() {
        UtilizationModelDynamicCustomized um;
        BiFunction<Double, Integer, Double> roundDouble = (value, places) -> {
            double a = 1;
            for (int i = 0; i < places; i++) {
                a *= 10;
            }
            return (double) Math.round(value * a) / a;
        };

        Random random = new Random();
        //Set a higher initial resource utilization in order to avoid fast overloaded or underloaded detection at the initial Vm placement level
        double initialUtilizationPercent =
            random.nextDouble() * (OVERUTILIZATION_THRESHOLD - (UNDERUTILIZATION_THRESHOLD + 0.01)) + (UNDERUTILIZATION_THRESHOLD + 0.01);

        um = new UtilizationModelDynamicCustomized(initialUtilizationPercent);
        um.setMaxResourceUtilization(1);
        um.setUtilizationUpdateSchedulingInterval(UTILIZATION_UPDATE_SCHEDULING_INTERVAL);

        um.setUtilizationUpdateFunction(utilizationModelDynamic -> {
            int randomInt = random.nextInt(100 - 1 + 1) + 1;
            double util = (double) randomInt / 100;
            return roundDouble.apply( util, 4);
        });

        return um;
    }

    protected List<Vm> createVms(List<Cloudlet> cloudletList) {
        List<Vm> vmList = new ArrayList<>();

        cloudletList.forEach(cloudlet -> {
            Vm vm;
            switch ((int) cloudlet.getNumberOfPes()) {
                case 1:
                    vm = new VmInstance1_A1_Medium().createVm();
                    cloudlet.setVm(vm);
                    vmList.add(vm);
                    break;
                case 2:
                    vm = new VmInstance2_C4_Large().createVm();
                    cloudlet.setVm(vm);
                    vmList.add(vm);
                    break;
                case 4:
                    vm = new VmInstance3_C4_xLarge().createVm();
                    cloudlet.setVm(vm);
                    vmList.add(vm);
                    break;
                case 8:
                    vm = new VmInstance4_C4_2xLarge().createVm();
                    cloudlet.setVm(vm);
                    vmList.add(vm);
                    break;
                default:
                    vm = new VmInstance5_C4_4xLarge().createVm();
                    cloudlet.setVm(vm);
                    cloudlet.setNumberOfPes(vm.getNumberOfPes());
                    vmList.add(vm);
            }
        });

        if (vmList.isEmpty()) {
            throw new IllegalStateException("The Vm list of a data center could not be empty!");
        }

        return vmList;
    }

    protected void simulationClocktickListener(EventInfo info) {
        dynamicWorkloadSubmission(info.getTime());
    }

    protected void dynamicWorkloadSubmission(final double clock) {
        if (clock - lastVmListSubmissionTime < VM_SUBMISSION_INTERVAL && clock > VM_SUBMISSION_INTERVAL) {
            return;
        }

        //updating the last time that some VM requests were submitted to the cloud brokers
        lastVmListSubmissionTime = clock;

        final int numberOfSubmmitedWorkloads = broker1.getCloudletSubmittedList().size();
        if (numberOfSubmmitedWorkloads < cloudletList.size()) {
            List<Cloudlet> newCloudletList = new ArrayList<>();
            for (int i = numberOfSubmmitedWorkloads; i < cloudletList.size(); i++) {
                if (cloudletList.get(i).getSubmissionDelay() <= clock) {
                    cloudletList.get(i).setSubmissionDelay(0);
                    cloudletList.get(i).getVm().setSubmissionDelay(0);
                    newCloudletList.add(cloudletList.get(i));
                }
            }

            Collections.shuffle(newCloudletList);

            if (!newCloudletList.isEmpty()) {
                submitWorkloadsForBrokers(newCloudletList);
            }
        }
    }

    protected void submitWorkloadsForBrokers(List<Cloudlet> cloudletList) {
        Random random = new Random();
        List<Vm> provider1_vmList = new ArrayList<>();
        List<Cloudlet> provider1_cloudletList = new ArrayList<>();

        cloudletList.forEach(cloudlet -> {
            provider1_vmList.add(cloudlet.getVm());
            provider1_cloudletList.add(cloudlet);
        });

        if (!provider1_cloudletList.isEmpty()) {
            broker1.submitVmList(provider1_vmList);
            broker1.submitCloudletList(provider1_cloudletList);
        }
    }

    protected void generateExperimentalResults() {
        ExperimentalResults results =
            new ExperimentalResults(OUTPUT_DIRECTORY, Collections.singletonList(broker1), SIMULATION_START_TIME, LocalDateTime.now());

        results.generateResults();
    }
}
