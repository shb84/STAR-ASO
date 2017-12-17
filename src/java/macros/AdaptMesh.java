/*
 * This macro performs adaptive mesh refinement given a simulation that is 
 * already setup for a basic fixed mesh run. Specifically, it adds tables and 
 * field functions to implement the adaptive mesh refinement approach described 
 * in the paper by: 
* 
 *  Venditti et al., "Grid Adaptation for Functional Outputs: Application to 2D 
 *  Inviscid Flows," Journal of Computational Physics, 2002, Vol 176, pp. 40-69 
 * 
 * Last Update: 5/13/2017
 */

package macro;

// Starccm+ packages 
import framework.*;
import star.common.*;
/**
 *
 * @author shb
 */
public class AdaptMesh extends StarMacro {
    
    private Simulation simulation;
    private CFDModel cfd;
    private MeshAdaptation adapt;
    private int numberAdapationLevels;
    private int numberPrimalSteps;
    private int numberAdjointSteps;
    private int number1stOrderGMRESAdjointSteps;
    private int number2ndOrderGMRESAdjointSteps;
    private String saveAs;
    private boolean isSave;
    private boolean isWarmStart;
    
    public void execute() {
        // --- Create objects 
        simulation = getActiveSimulation();
        cfd = new CFDModel(simulation);
        adapt = new MeshAdaptation(simulation); 
        
        // --- Read user inputs
        setUserInputs();
        readCommandLineArgs(); // overwrites user inputs
        
        // --- Perform mesh adaptation
        if (isWarmStart){
            adapt.runWithWarmStart( numberAdapationLevels,
                                    numberPrimalSteps,
                                    number1stOrderGMRESAdjointSteps,
                                    number2ndOrderGMRESAdjointSteps);
        }
        else {adapt.run( numberAdapationLevels, numberPrimalSteps, numberAdjointSteps);}

        if (isSave){
            if (!(saveAs == null)){cfd.save(saveAs);}
            else {cfd.save();}
        }
    }
    
    // ----------------------- USER INPUTS START HERE --------------------------
    private void setUserInputs(){
        isSave = true;
        isWarmStart = true;
        // - Solver - 
        numberPrimalSteps  = 2000;
        numberAdjointSteps = 2000;
        number1stOrderGMRESAdjointSteps = 50;
        number2ndOrderGMRESAdjointSteps = 50;
        adapt.setPrimalCFL(10.0);
        adapt.setAdjointCFL(50.0);
        // - Mesh Adaptation - 
        numberAdapationLevels = 3;
        adapt.setAdaptMaxSizeChange(4.0);
        adapt.setAdaptTargetError(0.0005);
        // - Tell MeshAdaption object where to find these simulation objects - 
        adapt.setAdaptiveMeshCostFunctionName("CD");
        adapt.setAdaptiveMeshCostFunctionReportName("CD");
        adapt.setFluidRegionName("Fluid");
        adapt.setMeshOperationNameName("Automated Mesh");
        adapt.setMeshRefinementTableName("Mesh Refinement");
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