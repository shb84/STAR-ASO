/*
 * This macro creates an airfoil simulation and runs it. It handles both 
 * inviscid and inviscid assumptions. Use cases:
 *
 * 1) You want to create a 2D airfoil simulation from scratch given only the 
 *    airfoil coordinates --> provide "airfoilCoordinatesCSVFile"
 * 2) You want to generate the mesh and run it (basic run) 
 * 3) You want to prepare an airfoil simulation with control points for shape 
 *    optimization --> provide "controlPointCSVFile"
 */

package macro;

// Starccm+ packages 
import framework.*;
import star.common.*;
import java.io.*;
/**
 *
 * @author shb
 */
public class CreateAndRunAirfoil extends StarMacro {

    private Simulation simulation;
    private Airfoil2D airfoil;
    private String airfoilCoordinatesCSVFile;
    private String controlPointCSVFile;
    private boolean isInviscid;
    private int numberOfPrimalSteps;
    private int numberOfAdjointSteps;
    
    public void execute() { 
        
        // --- Create objects 
        simulation = getActiveSimulation();
        airfoil = new Airfoil2D(simulation); 
        
        // --- Read user inputs
        readUserInputs();
        readCommandLineArgs(); // overrides user inputs

         // --- Create simulation from scratch
        airfoil.clear();
        airfoil.createGeometry(airfoilCoordinatesCSVFile);
        airfoil.createMesher(isInviscid,"Automated Mesh");
        airfoil.createSolvers(isInviscid);
        airfoil.createForceAndMomentReports(isInviscid);
        airfoil.createControlPoints(controlPointCSVFile);

        // --- Mesh and run
        airfoil.generateMesh();
        airfoil.runPrimalSolver(numberOfPrimalSteps);
        airfoil.runAdjointSolver(numberOfAdjointSteps);

        // --- Save
        airfoil.save();
    }
    
    // ----------------------- USER INPUTS START HERE --------------------------
    private void readUserInputs(){
        // - Local class variables - 
        airfoilCoordinatesCSVFile = "AirfoilXYZ.csv";
        controlPointCSVFile = "ControlPointXYZ.csv";
        isInviscid = false; 
        numberOfPrimalSteps = 2000; 
        numberOfAdjointSteps = 2000; 
        // - Names - 
        airfoil.setAirfoilPartName("Airfoil");
        airfoil.setFarfieldPartName("Farfield");
        airfoil.setDomainPartName("Domain");
        airfoil.setFluidRegionName("Fluid");
        // - Mesh controls - 
        airfoil.setAirfoilMinimumSurfaceSize(0.0001); // m
        airfoil.setAirfoilTargetSurfaceSize( 0.01); // m
        airfoil.setFarfieldMinimumSurfaceSize(2.5); // m
        airfoil.setFarfieldTargetSurfaceSize(100.); // m
        airfoil.setTrailingEdgeMinimumSurfaceSize(0.0001); // m
        airfoil.setTrailingEdgeTargetSurfaceSize(0.002); // m
        airfoil.setAirfoilNumberPrismLayers(20);
        airfoil.setAirfoilPrismWallThickness(0.000005); // m
        airfoil.setAirfoilPrismTotalThickness(0.01); // m
        // - Flight conditions - 
        airfoil.setReferencePressure(101325.0); // Pa
        airfoil.setFreestreamMachNumber(0.725);
        airfoil.setFreeStreamViscosity(0.0000458); // Pa-s
        airfoil.setFreestreamSpeedOfSound(347.0); // m/s
        airfoil.setFreestreamGaugePressure(0.0); // Pa
        airfoil.setFreestreamTemperature(300.0); // K
        airfoil.setFreestreamDensity(1.177); // kg/m3
        airfoil.setAngleOfAttack(2.31); // deg
        // - Solver - 
        airfoil.setPrimalCFL(10.0);
        airfoil.setAdjointCFL(25.0);
        // - Reports - 
        airfoil.setMomentOrigin(0.25);
    }
    // ----------------------- USER INPUTS END HERE ----------------------------

    // Method that reads in command line arguments
    private void readCommandLineArgs(){

        String referencePressure = System.getProperty("Pref"); // Pa
        if (!(referencePressure == null)){
            simulation.println("Reference Pressure: " + referencePressure + " Pa");
            double Pref = Double.parseDouble(referencePressure);
            airfoil.setReferencePressure(Pref);
        }

        String freestreamGaugePressure = System.getProperty("dP"); // Pa
        if (!(freestreamGaugePressure == null)){
            simulation.println("Freestream Gauge Pressure: " + freestreamGaugePressure + " Pa");
            double dP = Double.parseDouble(freestreamGaugePressure);
            airfoil.setFreestreamGaugePressure(dP);
        }

        String freestreamMachNumber = System.getProperty("M"); // -
        if (!(freestreamMachNumber == null)){
            simulation.println("Freestream Mach Number: " + freestreamMachNumber);
            double M = Double.parseDouble(freestreamMachNumber);
            airfoil.setFreestreamMachNumber(M);
        }

        String freestreamViscosity = System.getProperty("mu"); // Pa-s
        if (!(freestreamViscosity == null)){
            simulation.println("Freestream Dynamic Viscosity: " + freestreamViscosity + " Pa-s");
            double mu = Double.parseDouble(freestreamViscosity);
            airfoil.setFreeStreamViscosity(mu);
        }

        String freestreamSpeedOfSound = System.getProperty("a"); // m/s
        if (!(freestreamSpeedOfSound == null)){
            simulation.println("Freestream Speed Of Sound: " + freestreamSpeedOfSound + " m/s");
            double a = Double.parseDouble(freestreamSpeedOfSound);
            airfoil.setFreestreamSpeedOfSound(a);
        }

        String freestreamTemperature = System.getProperty("T"); // K
        if (!(freestreamTemperature == null)){
            simulation.println("Freestream Temperature: " + freestreamTemperature + " K");
            double T = Double.parseDouble(freestreamTemperature);
            airfoil.setFreestreamTemperature(T);
        }

        String freestreamDensity = System.getProperty("rho"); // kg/m3
        if (!(freestreamDensity == null)){
            simulation.println("Freestream Density: " + freestreamDensity + " kg/m3");
            double rho = Double.parseDouble(freestreamDensity);
            airfoil.setFreestreamDensity(rho);
        }

        String angleOfAttack = System.getProperty("alpha"); // deg
        if (!(angleOfAttack == null)){
            simulation.println("Freestream angleOfAttack: " + angleOfAttack + " deg");
            double alpha = Double.parseDouble(angleOfAttack);
            airfoil.setAngleOfAttack(alpha);
        }
    }
}
