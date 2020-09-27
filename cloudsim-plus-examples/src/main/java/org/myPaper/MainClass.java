package org.myPaper;

import org.myPaper.programs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class MainClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainClass.class.getSimpleName());
    private enum PROGRAM{BFD, FFD, CRA_DP, ERA_DP, FA_DP, ERA_FFD, OUR_ALGO};
    private static String directory = null;
    private static int workloadsNumber = -1;

    public static void main(String[] arg) {
        LOGGER.info("Welcome to CloudSim Plus Simulation Toolkit.");
        LOGGER.info("Please choose one of the following programs for running the simulator: \n{}",
                "1. The BFD (Best Fit Decreasing) algorithm \n" +
                    "2. The FFD (First Fit Decreasing) algorithm \n" +
                    "3. The CRA-DP Algorithm (Khosravi 2017 paper) \n" +
                    "4. The ERA-DP Algorithm (Khosravi 2017 paper) \n" +
                    "5. The FA-DP Algorithm (Khosravi 2017 paper) \n" +
                    "6. The ERA-DP-FFD Algorithm \n" +
                    "7. The OUR-ALGO");

        Scanner scanner = new Scanner(System.in);

        PROGRAM program = null;

        while (program == null) {
            System.out.println("Program Id: " );

            String id = scanner.nextLine();

            try {
                switch (Integer.parseInt(id)) {
                    case 1:
                        program = PROGRAM.valueOf("BFD");
                        break;
                    case 2:
                        program = PROGRAM.valueOf("FFD");
                        break;
                    case 3:
                        program = PROGRAM.valueOf("CRA_DP");
                        break;
                    case 4:
                        program = PROGRAM.valueOf("ERA_DP");
                        break;
                    case 5:
                        program = PROGRAM.valueOf("FA_DP");
                        break;
                    case 6:
                        program = PROGRAM.valueOf("ERA_FFD");
                        break;
                    case 7:
                        program = PROGRAM.valueOf("OUR_ALGO");
                        break;
                    default:
                        LOGGER.warn("The given program Id {} does not exist!", id);
                }
            } catch (NumberFormatException e) {
                showInvalidInputError(id);
            }
        }

        LOGGER.info("Please insert the directory that you want to save the experimental results.");

        while (directory == null) {
            System.out.println("Directory: ");
            directory = scanner.nextLine();

            if (!Files.exists(Paths.get(directory))) {
                LOGGER.warn("The given directory {} does not exist!", directory);
                directory = null;
            }
        }

        LOGGER.info("Please insert the total number of VM creation requests in range (0,14000] during the simulation time.");

        while (workloadsNumber == -1) {
            System.out.println("Number of requests: ");
            String numberOfReqs = scanner.nextLine();

            try {

                if (Integer.parseInt(numberOfReqs) > 0 && Integer.parseInt(numberOfReqs) <= 14000) {
                    workloadsNumber = Integer.parseInt(numberOfReqs);
                }else {
                    showInvalidInputError(numberOfReqs);
                }

            } catch (NumberFormatException e) {
                showInvalidInputError(numberOfReqs);
            }
        }

        LOGGER.info("You chose the destination {} for the {} program successfully with {} VM creation requests.",
            directory,
            program.toString(),
            workloadsNumber);

        LOGGER.info("To change more settings please use the ParentClass.");

        runProgram(program);
    }

    private static void runProgram(final PROGRAM program) {
        if (program == null) {
            throw new IllegalStateException("The program enum could not be null!");
        }

        switch (program) {
            case BFD:
                new BFDProgram(directory, workloadsNumber);
                break;
            case FFD:
                new FFDProgram(directory, workloadsNumber);
                break;
            case CRA_DP:
                new CraDpProgram(directory, workloadsNumber);
                break;
            case ERA_DP:
                new EraDpProgram(directory, workloadsNumber);
                break;
            case FA_DP:
                new FaDpProgram(directory, workloadsNumber);
                break;
            case ERA_FFD:
                new EraFfdProgram(directory, workloadsNumber);
                break;
            case OUR_ALGO:
                new OurProgram(directory, workloadsNumber);
                break;
            default:
                throw new IllegalStateException("The requested program was not found!");
        }
    }

    private static boolean askYesNoQuestion(final String question) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            LOGGER.info(question + " [y|n]");

            String answer = scanner.nextLine();

            if (answer.equals("y")) {
                return true;
            } else if (answer.equals("n")) {
                return false;
            }else {
                LOGGER.warn("Could not recognise the given answer!");
            }
        }
    }

    private static void showInvalidInputError(final String input) {
        LOGGER.warn("The given input {} is invalid!", input);
    }
}
