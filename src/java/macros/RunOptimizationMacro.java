/*
 * This macro updates an existing simulation, already setup for optimization, 
 * by reading in new variable values, running, and writing output functions 
 * and gradients to file. This macro is intended to be used with an external 
 * optimizer that treats Star-CCM+ as a blackbox and exchanges information 
 * through by reading, writing, and parsing local files. 
 * 
 * Specifically, only two read/write CSV files are expected: 
 * 1) IndependentVariables.csv <---- contains input design variables 
 * 2) DependentVariables.csv ----> contains output functions 
 * 
 * The structure of the files is pretty obvious given an example, but more 
 * information will be provided via documentation eventually. In the meantime, 
 * email stevenberguin@gatech.edu for any questions. 
 * 
 * Last Update: 5/13/2017
 */

package macro;

// Starccm+ packages 
import framework.*;
import java.io.*;
import star.common.*;
/**
 *
 * @author shb
 */
public class RunOptimizationMacro extends StarMacro {

    // Class objects
    private Simulation  sim;
    private CFDModel    cfd;
    private Problem     problem;

    // Class variables
    private String independentVariableFilepath;
    private String dependentVariableFilepath;
    private int numberPrimalSteps;
    private int number1stOrderGMRESAdjointSteps;
    private int number2ndOrderGMRESAdjointSteps;
    private boolean isRunAdjoint;
    private String fluidRegionName;
    private String freestreamBoundaryName;
    private String PhysicsContinuumName;
    private boolean isFlow2D;


    public void execute() {

        // --- Create objects
        sim = getActiveSimulation();
        cfd = new CFDModel(sim);
        problem = new Problem(cfd);

        // --- Read user inputs
        setUserInputs();
        cfd.setFluidRegionName(fluidRegionName);
        cfd.setFreestreamBoundaryName(freestreamBoundaryName);

        // --- Apply flight conditions by linking global parameters to IC, BC, reports, etc.
        cfd.set2DFlag(isFlow2D);
        cfd.flightCondition.linkGlobalParameters(fluidRegionName,freestreamBoundaryName,PhysicsContinuumName);

        // --- Mesh
//        cfd.mesher.mesh();
//        cfd.save();

        // --- Run
        checkForCommandLineArgs(); // overrides user inputs
        problem.readIndependentVariables(independentVariableFilepath);
        if (!isRunAdjoint) {
            cfd.solver.restartPrimal(numberPrimalSteps); // clear histories and run
            cfd.save();
        }
        else {
            cfd.solver.runAdjointWithWarmStart(number1stOrderGMRESAdjointSteps,number2ndOrderGMRESAdjointSteps);
            cfd.save();
        }
        try{problem.writeDependentVariables(dependentVariableFilepath,isRunAdjoint);}
        catch (IOException e){e.getMessage();}
    }

    // ----------------------- USER INPUTS START HERE --------------------------
    private void setUserInputs(){
        // - Files -
        independentVariableFilepath = sim.getSessionDir() + File.separator + "IndependentVariables.csv";
        dependentVariableFilepath   = sim.getSessionDir() + File.separator + "DependentVariables.csv";
        // - Flags -
        isRunAdjoint = true; // use this to deactivate gradient computation (e.g. during line search)
        isFlow2D = true;
        cfd.flightCondition.setEulerFlag(true);
        // - Names -
        fluidRegionName = "Fluid";
        freestreamBoundaryName = "Domain.Farfield";
        PhysicsContinuumName = "Physics";
        // - Solver -
        numberPrimalSteps = 1000;
        number1stOrderGMRESAdjointSteps = 25; // new <-- warm-start
        number2ndOrderGMRESAdjointSteps = 15; // new
        cfd.solver.setPrimalCFL(10.0);
        cfd.solver.setAdjointCFL(25.0);
        // - Flight conditions -
        cfd.flightCondition.setDragCoefficientReportName("CD");
        cfd.flightCondition.setLiftCoefficientReportName("CL");
        cfd.flightCondition.setReferencePressure(101325.0); // Pa
        cfd.flightCondition.setMachNumber(0.73);
        cfd.flightCondition.setFreestreamDynamicViscosity(0.00001789); // Pa-s
        cfd.flightCondition.setFreestreamSpeedOfSound(340.0); // m/s
        cfd.flightCondition.setFreestreamGaugePressure(0.0); // Pa
        cfd.flightCondition.setFreestreamTemperature(288.15); // K
        cfd.flightCondition.setFreestreamDensity(1.225); // kg/m3
        cfd.flightCondition.setAngleOfAttack(2.00); // deg // overriden by "IndependentVariables.csv" if "alpha" present
    }
    // ----------------------- USER INPUTS END HERE ----------------------------

    private void checkForCommandLineArgs(){
        String adj_flag = System.getProperty("adj_flag");
        if (!(adj_flag == null)){isRunAdjoint = Boolean.valueOf(adj_flag);}
    }
}
