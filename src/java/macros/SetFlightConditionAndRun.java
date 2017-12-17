/*
 * This macro updates the flight conditions in an existing simulation and runs it.  
 *
 * Last Update: 5/13/2017
 */

package macro;

// Starccm+ packages
import framework.*;
import java.io.*;
import java.util.*;
import star.common.*;
/**
 *
 * @author shb
 */
public class SetFlightConditionAndRun extends StarMacro {

    // Class objects
    private Simulation  simulation;
    private CFDModel    cfd;

    // Class variables
    private int numberPrimalSteps;
    private int numberAdjointSteps;
    private boolean isRunAdjoint;
    private boolean isGenerateMesh;
    private String fluidRegionName;
    private String freestreamBoundaryName;
    private String PhysicsContinuumName;
    private  boolean isFlow2D;
    private int number1stOrderGMRESAdjointSteps;
    private int number2ndOrderGMRESAdjointSteps;
    private String saveAs;
    private boolean isSave;
    private boolean isWarmStart;


    public void execute() {

        // --- Create objects
        simulation = getActiveSimulation();
        cfd = new CFDModel(simulation);

        // --- Read user inputs
        setUserInputs();
        cfd.setFluidRegionName(fluidRegionName);
        cfd.setFreestreamBoundaryName(freestreamBoundaryName);

        // --- Flight Conditions 
        readCommandLineArgs(); // overwrites user inputs
        cfd.flightCondition.set2DFlag(isFlow2D);
        cfd.flightCondition.linkGlobalParameters(fluidRegionName,freestreamBoundaryName,PhysicsContinuumName);

		// --- Mesh 
        if (isGenerateMesh){cfd.mesher.mesh();}
		
        // --- Primal
        cfd.solver.restartPrimal(numberPrimalSteps); // clears history but not solution 

        // --- Adjoint
        if (isRunAdjoint){
			if (isWarmStart){
				cfd.solver.runAdjointWithWarmStart( number1stOrderGMRESAdjointSteps, number2ndOrderGMRESAdjointSteps);
			}
			else {cfd.solver.runAdjoint(numberAdjointSteps);}
		}
		
        // --- Save
        if (isSave){
            if (!(saveAs == null)){cfd.save(saveAs);}
            else {cfd.save();}
        }
    }

    // ----------------------- USER INPUTS START HERE --------------------------
    private void setUserInputs(){
        // - Flags -
        isGenerateMesh = false; 
        isRunAdjoint = false; 
        isSave = true;
        isWarmStart = true;
        isFlow2D = true;
        // - Names -
        fluidRegionName = "Fluid";
        freestreamBoundaryName = "Domain.Farfield";
        PhysicsContinuumName = "Physics";
        // - Solver -
        numberPrimalSteps  = 2000;
        numberAdjointSteps = 2000;
        number1stOrderGMRESAdjointSteps = 50;
        number2ndOrderGMRESAdjointSteps = 50;
        cfd.solver.setPrimalCFL(5.0);
        cfd.solver.setAdjointCFL(10.0);
        // - Flight conditions - (overwritten by command line inputs)
        cfd.flightCondition.setDragCoefficientReportName("CD");
        cfd.flightCondition.setLiftCoefficientReportName("CL");
        cfd.flightCondition.setEulerFlag(false);
        cfd.flightCondition.setReferencePressure(101325.0); // Pa
        cfd.flightCondition.setMachNumber(0.725);
        cfd.flightCondition.setFreestreamDynamicViscosity(0.0000458); // Pa-s
        cfd.flightCondition.setFreestreamSpeedOfSound(347.0); // m/s
        cfd.flightCondition.setFreestreamGaugePressure(0.0); // Pa
        cfd.flightCondition.setFreestreamTemperature(300.0); // K
        cfd.flightCondition.setFreestreamDensity(1.177); // kg/m3
        cfd.flightCondition.setAngleOfAttack(2.31); // deg
    }
    // ----------------------- USER INPUTS END HERE ----------------------------
	
    // Method that reads in command line arguments
    private void readCommandLineArgs(){

        String referencePressure = System.getProperty("Pref"); // Pa
        if (!(referencePressure == null)){
            simulation.println("Reference Pressure: " + referencePressure + " Pa");
            double Pref = Double.parseDouble(referencePressure);
            cfd.flightCondition.setReferencePressure(Pref);
        }

        String freestreamGaugePressure = System.getProperty("dP"); // Pa
        if (!(freestreamGaugePressure == null)){
            simulation.println("Freestream Gauge Pressure: " + freestreamGaugePressure + " Pa");
            double dP = Double.parseDouble(freestreamGaugePressure);
            cfd.flightCondition.setFreestreamGaugePressure(dP);
        }

        String freestreamMachNumber = System.getProperty("M"); // -
        if (!(freestreamMachNumber == null)){
            simulation.println("Freestream Mach Number: " + freestreamMachNumber);
            double M = Double.parseDouble(freestreamMachNumber);
            cfd.flightCondition.setMachNumber(M);
        }

        String freestreamViscosity = System.getProperty("mu"); // Pa-s
        if (!(freestreamViscosity == null)){
            simulation.println("Freestream Dynamic Viscosity: " + freestreamViscosity + " Pa-s");
            double mu = Double.parseDouble(freestreamViscosity);
            cfd.flightCondition.setFreestreamDynamicViscosity(mu);
        }

        String freestreamSpeedOfSound = System.getProperty("a"); // m/s
        if (!(freestreamSpeedOfSound == null)){
            simulation.println("Freestream Speed Of Sound: " + freestreamSpeedOfSound + " m/s");
            double a = Double.parseDouble(freestreamSpeedOfSound);
            cfd.flightCondition.setFreestreamSpeedOfSound(a);
        }

        String freestreamTemperature = System.getProperty("T"); // K
        if (!(freestreamTemperature == null)){
            simulation.println("Freestream Temperature: " + freestreamTemperature + " K");
            double T = Double.parseDouble(freestreamTemperature);
            cfd.flightCondition.setFreestreamTemperature(T);
        }

        String freestreamDensity = System.getProperty("rho"); // kg/m3
        if (!(freestreamDensity == null)){
            simulation.println("Freestream Density: " + freestreamDensity + " kg/m3");
            double rho = Double.parseDouble(freestreamDensity);
            cfd.flightCondition.setFreestreamDensity(rho);
        }

        String angleOfAttack = System.getProperty("alpha"); // deg
        if (!(angleOfAttack == null)){
            simulation.println("Freestream angleOfAttack: " + angleOfAttack + " deg");
            double alpha = Double.parseDouble(angleOfAttack);
            cfd.flightCondition.setAngleOfAttack(alpha);
        }

        String save = System.getProperty("save");
        if (!(save == null)){
            simulation.println("Save as: " + save);
            saveAs = save;
            isSave = true;
        }
    }
}