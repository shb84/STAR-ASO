/*
 * This class provides methods to create and run an airfoil in Star-CCM+.  
 */

package framework; 

import star.common.*;
import java.util.*; 
import java.io.*; 

/**
 *
 * @author shb
 */
public class Airfoil2D {
    
    // -------------------------------------------------------------------------
    // ---------------------- A T T R I B U T E S ------------------------------
    // -------------------------------------------------------------------------
    
    private Simulation simulation;
    private Geometry2D geometry; 
    private FlightCondition flightCondition;
    private PolygonalMesher mesher; 
    private Solver solver; 
    private Morpher morpher;
    private Toolbox toolbox; 
    
    // - Geometry - 
    private String m_airfoilPartName; 
    private String m_farfieldPartName; 
    private String m_domainPartName; 
    private String m_fluidRegionName; 
    
    // - Mesher -
    private double m_airfoilMinimumSurfaceSize; // m
    private double m_airfoilTargetSurfaceSize; // m
    private double m_farfieldMinimumSurfaceSize; // m
    private double m_farfieldTargetSurfaceSize; // m
    private int    m_airfoilNumberPrismLayers; 
    private double m_airfoilPrismWallThickness; // m
    private double m_airfoilPrismTotalThickness;  // m
    private double m_trailingEdgeMinimumSurfaceSize; // m 
    private double m_trailingEdgeTargetSurfaceSize; // m
    private double m_defaultPrismLayerRelativeThickness; 
    
    // - Flight Conditions -  
    private double m_referencePressure; // Pa
    private double m_machNumber; 
    private double m_angleOfAttack; // deg
    private double m_dynamicViscosity; // Pa-s
    private double m_pressure; // Pa 
    private double m_temperature; // K
    private double m_density; // kg/m3
    private double m_speedOfSound; // m/s
    
    // - Solvers -
    private double m_primalCFL; 
    private double m_adjointCFL; 
    
    // - Reports -
    private double m_momentOrigin; 
            
    // -------------------------------------------------------------------------
    // ---------------------- C O N S T R U C T O R ----------------------------
    // -------------------------------------------------------------------------
    
    public Airfoil2D(Simulation sim)  {
        // - Objects -
        simulation = sim; 
        geometry = new Geometry2D(sim); 
        flightCondition = new FlightCondition(sim);
        flightCondition.set2DFlag(true);
        toolbox = new Toolbox(sim);
        mesher = new PolygonalMesher(sim); 
        solver = new Solver(sim); 
        morpher = new Morpher(sim); 
        // - Names - 
        m_airfoilPartName = "Airfoil"; 
        m_farfieldPartName = "Farfield"; 
        m_domainPartName = "Domain";
        m_fluidRegionName = "Fluid"; 
        // - Mesh controls -
        m_airfoilMinimumSurfaceSize = 0.0001; // m
        m_airfoilTargetSurfaceSize = 0.001; // m
        m_farfieldMinimumSurfaceSize = 2.5; // m
        m_farfieldTargetSurfaceSize = 100.; // m
        m_airfoilNumberPrismLayers = 46;  
        m_airfoilPrismWallThickness = 0.000005; // m
        m_airfoilPrismTotalThickness = 0.02; // m
        m_trailingEdgeTargetSurfaceSize = 0.002; // m
        m_trailingEdgeMinimumSurfaceSize = 0.0001; // m 
        m_defaultPrismLayerRelativeThickness = 0.01; 
        // - Flight conditions - 
        m_referencePressure = 101325.0; // Pa
        m_machNumber = 0.25;
        m_angleOfAttack = 2.0; // deg
        m_dynamicViscosity = 0.00001789; // Pa-s
        m_pressure = 0.0; // Pa (gauge pressure = P - Pref)
        m_temperature = 288.15; // K
        m_density = 1.225; // kg/m3
        m_speedOfSound = 340.0; // m/s
        // - Solvers - 
        m_primalCFL = 5.0; 
        m_adjointCFL = 5.0; 
        // - Reports - 
        m_momentOrigin = 0.0; 
    } 
     
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    //  Names 
    public void setAirfoilPartName(String s){m_airfoilPartName = s; }
    public void setFarfieldPartName(String s){m_farfieldPartName = s;}
    public void setDomainPartName(String s){m_domainPartName = s; }
    public void setFluidRegionName(String s){m_fluidRegionName = s;}
    
    public String getAirfoilPartName(){return m_airfoilPartName; }
    public String getFarfieldPartName(){return m_farfieldPartName;}
    public String getDomainPartName(){return m_domainPartName; }
    public String getFluidRegionName(){return m_fluidRegionName;}
    
    // Mesh controls
    public void setAirfoilMinimumSurfaceSize(double d){m_airfoilMinimumSurfaceSize = d;}
    public void setAirfoilTargetSurfaceSize(double d){ m_airfoilTargetSurfaceSize = d;}
    public void setFarfieldMinimumSurfaceSize(double d){m_farfieldMinimumSurfaceSize = d;}
    public void setFarfieldTargetSurfaceSize(double d){m_farfieldTargetSurfaceSize = d;}
    public void setTrailingEdgeMinimumSurfaceSize(double d){m_trailingEdgeMinimumSurfaceSize = d;}
    public void setTrailingEdgeTargetSurfaceSize(double d){m_trailingEdgeTargetSurfaceSize = d;}
    public void setAirfoilNumberPrismLayers(int i){m_airfoilNumberPrismLayers = i;}
    public void setAirfoilPrismWallThickness(double d){m_airfoilPrismWallThickness = d;}
    public void setAirfoilPrismTotalThickness(double d){m_airfoilPrismTotalThickness = d;}
    
    public double getAirfoilMinimumSurfaceSize(){return m_airfoilMinimumSurfaceSize;}
    public double getAirfoilTargetSurfaceSize(){return m_airfoilTargetSurfaceSize;}
    public double getFarfieldMinimumSurfaceSize(){return m_farfieldMinimumSurfaceSize;}
    public double getFarfieldTargetSurfaceSize(){return m_farfieldTargetSurfaceSize;}
    public double getTrailingEdgeSurfaceSize(){return m_trailingEdgeMinimumSurfaceSize;}
    public double getTrailingEdgeTargetSurfaceSize(){return m_trailingEdgeTargetSurfaceSize;}
    public int getAirfoilNumberPrismLayers(){return m_airfoilNumberPrismLayers;}
    public double getAirfoilPrismWallThickness(){return m_airfoilPrismWallThickness;}
    public double getAirfoilPrismTotalThickness(){return m_airfoilPrismTotalThickness;}
    
    // Flight conditions 
    public void setReferencePressure(double d){
        m_referencePressure = d;
        flightCondition.setReferencePressure(d); // Pa
    }
    public void setFreestreamMachNumber(double d){
        m_machNumber = d;
        flightCondition.setMachNumber(d);
    }
    public void setFreeStreamViscosity(double d){
        m_dynamicViscosity = d;
        flightCondition.setFreestreamDynamicViscosity(d); // Pa-s
    }
    public void setAngleOfAttack(double d){
        m_angleOfAttack = d; 
        flightCondition.setAngleOfAttack(d); // deg
    }
    public void setFreestreamGaugePressure(double d){
        m_pressure = d;
        flightCondition.setFreestreamGaugePressure(d); // Pa
    }
    public void setFreestreamTemperature(double d){
        m_temperature = d;
        flightCondition.setFreestreamTemperature(d); // K
    }
    public void setFreestreamDensity(double d){
        m_density = d;
        flightCondition.setFreestreamDensity(d); // kg/m3
    }
    public void setFreestreamSpeedOfSound(double d){
        m_speedOfSound = d;
        flightCondition.setFreestreamSpeedOfSound(d); // m/s
    }
    
    public double getReferencePressure(){return m_referencePressure;}
    public double getFreestreamMachNumber(){return m_machNumber;}
    public double getFreeStreamViscosity(){return m_dynamicViscosity;}
    public double getAngleOfAttack(){return m_angleOfAttack;}
    public double getFreestreamGaugePressure(){return m_pressure;}
    public double getFreestreamTemperature(){return m_temperature;}
    public double getFreestreamDensity(){return m_density;}
    public double getFreestreamSpeedOfSound(){return m_speedOfSound;}
    
    // Solvers 
    public void setPrimalCFL(double d){m_primalCFL = d;}
    public void setAdjointCFL(double d){m_adjointCFL = d;}
    
    public double getPrimalCFL(){return m_primalCFL;}
    public double getAdjointCFL(){return m_adjointCFL;}
    
    // Reports 
    public void setMomentOrigin(double d){m_momentOrigin = d;}
    
    public double getMomentOrigin(){return m_momentOrigin;}
    
    // This method clears any previous simulation 
    public void clear(){
        toolbox.deleteAdjointCostFunctions();
        solver.clear(); 
        mesher.clear(); 
        geometry.clear(); 
        toolbox.deletePlots();
        toolbox.deleteMonitors();
        toolbox.deleteReports(); 
    }
    
    // This method creates an airfoil 
    public void createGeometry(String airfoilCoordinateCSVFileName){
        geometry.setAirfoilPartName(m_airfoilPartName);
        geometry.setFarfieldPartName(m_farfieldPartName);
        geometry.setAirfoilPartSurfaceName(m_airfoilPartName);
        geometry.setDomainPartName(m_domainPartName);
        geometry.setFarfieldPartSurfaceName(m_farfieldPartName);
        geometry.createAirfoil(simulation.getSessionDir() + File.separator + airfoilCoordinateCSVFileName);
    }
    
    // This method creates a mesher but does not generate it
    public void createMesher(boolean isInviscid,String meshOperationName){ 
        mesher.setDomainPartName(m_domainPartName);
        mesher.setFluidRegionName(m_fluidRegionName);
        mesher.setMeshOperationName(meshOperationName);
        mesher.assignPartsToRegion(); 
        mesher.createMeshOperation(isInviscid);
        mesher.customizeMesh(m_airfoilPartName,"Target Surface Size",m_airfoilTargetSurfaceSize); 
        mesher.customizeMesh(m_airfoilPartName,"Minimum Surface Size",m_airfoilMinimumSurfaceSize);
        mesher.customizeMesh(m_farfieldPartName,"Target Surface Size",m_farfieldTargetSurfaceSize); 
        mesher.customizeMesh(m_farfieldPartName,"Minimum Surface Size",m_farfieldMinimumSurfaceSize);
        if (!isInviscid){ 
            mesher.customizeMesh(m_airfoilPartName, "Number of Prism Layers", m_airfoilNumberPrismLayers);
            mesher.customizeMesh(m_airfoilPartName, "Prism Layer Near Wall Thickness", m_airfoilPrismWallThickness);
            mesher.customizeMesh(m_airfoilPartName, "Prism Layer Total Thickness", m_airfoilPrismTotalThickness);
            mesher.customizeMesh(m_farfieldPartName, "Number of Prism Layers",0.0); // disable prism layer on farfield
        }
    }
    
    // - Solver -
    public void createSolvers(boolean isInviscid){
        solver.createPhysicsContinuum("Physics",isInviscid,m_referencePressure);  
        solver.addRegionToPhysics(m_fluidRegionName);
        solver.createPrimalSolver(m_primalCFL); 
        solver.createAdjointSolver(m_adjointCFL); 
        flightCondition.setEulerFlag(isInviscid);
        solver.setBoundaryConditionType(m_domainPartName + "." + m_farfieldPartName,"Freestream"); 
        solver.setBoundaryConditionType(m_domainPartName + "." + m_airfoilPartName,"Wall");
        flightCondition.linkGlobalParameters(m_fluidRegionName,m_domainPartName + "." + m_farfieldPartName,"Physics");
    }
    
    // - Force and Moment Reports - 
    public void createForceAndMomentReports(boolean isInviscid){  
        ArrayList<String> boundaryNames = new ArrayList(); 
        boundaryNames.add(m_domainPartName + "." + m_airfoilPartName);
        createDragCoefficientReport("CD",boundaryNames,isInviscid);
        createLiftCoefficientReport("CL",boundaryNames,isInviscid);
        createMomentCoefficientReport("CM",boundaryNames,isInviscid);
        flightCondition.setDragCoefficientReportName("CD");
        flightCondition.setLiftCoefficientReportName("CL");
        flightCondition.linkGlobalParameters(m_fluidRegionName,m_domainPartName + "." + m_farfieldPartName,"Physics");
    }

    // This method initializes arrays containing file information that is needed
    // by the framework upon optimization. It then calls the parent function of 
    // the same name, which sets up the morpher control points, etc. 
    public void createControlPoints(String controlPointTableFileName){
        ArrayList<String> morpherControlPointCSVFileNames = new ArrayList(); 
        ArrayList<String> adjointCostFunctionNames = new ArrayList(); 
        ArrayList<String> morpherFloatingBoundaryNames = new ArrayList(); 
        morpher.clear();
        toolbox.createForceCostFunction("CD","CD");
        toolbox.createForceCostFunction("CL","CL");
        toolbox.createMomentCostFunction("CM","CM");
        morpherControlPointCSVFileNames.add(controlPointTableFileName);
        adjointCostFunctionNames.add("CD");
        adjointCostFunctionNames.add("CL");
        adjointCostFunctionNames.add("CM");
        morpherFloatingBoundaryNames.add(m_domainPartName + "." + m_airfoilPartName);
        morpher.initialize(morpherControlPointCSVFileNames,
                           adjointCostFunctionNames,
                           morpherFloatingBoundaryNames); 
    }
    
    // This method generates the mesh 
    public void generateMesh(){mesher.mesh();}
    
    // This method runs the primal solver
    public void runPrimalSolver(int numberOfSteps){solver.runPrimal(numberOfSteps);}
    
    // This method runs the adjoint solver
    public void runAdjointSolver(int numberOfSteps){solver.runAdjoint(numberOfSteps);}

    public void saveas(String simName){
        simulation.saveState(simulation.getSessionDir() + File.separator + simName);
    }
    public void save(){simulation.saveState(simulation.getSessionPath());}
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // This method sets a drag and lift coefficient report
    private void createDragCoefficientReport(String reportName, 
                                              ArrayList<String> boundaryNames,
                                              boolean isInviscid){
        double[] flowDirection = new double[3]; 
        flowDirection[0] = Math.cos(Math.toRadians(m_angleOfAttack));
        flowDirection[1] = Math.sin(Math.toRadians(m_angleOfAttack));
        flowDirection[2] = 0.0; 
        toolbox.createForceCoefReport(  reportName,
                                        m_fluidRegionName,
                                        boundaryNames,
                                        flowDirection,
                                        0.0,
                                        m_density,
                                        m_machNumber*m_speedOfSound,
                                        1.0,
                                        isInviscid); 
    }
     
    // This method sets a drag and lift coefficient report
    private void createLiftCoefficientReport(String reportName, 
                                             ArrayList<String> boundaryNames,
                                             boolean isInviscid){
       double[] flowDirection = new double[3]; 
       flowDirection[0] = -Math.sin(Math.toRadians(m_angleOfAttack));
       flowDirection[1] =  Math.cos(Math.toRadians(m_angleOfAttack));
       flowDirection[2] = 0.0; 
       toolbox.createForceCoefReport(   reportName,
                                        m_fluidRegionName,
                                        boundaryNames,
                                        flowDirection,
                                        0.0,
                                        m_density,
                                        m_machNumber*m_speedOfSound,
                                        1.0,
                                        isInviscid); 
    }
    
    // This method sets a drag and lift coefficient report
    private void createMomentCoefficientReport(String reportName, 
                                               ArrayList<String> boundaryNames,
                                               boolean isInviscid){
       double[] axis = new double[3]; 
       axis[0] = 0.0;
       axis[1] = 0.0;
       axis[2] = -1.0; 
       double[] origin = new double[3];
       origin[0] = m_momentOrigin; 
       origin[1] = 0.0; 
       origin[2] = 0.0; 
       toolbox.createMomentCoefReport(reportName,
                                      m_fluidRegionName,
                                      boundaryNames,
                                      axis,
                                      origin, 
                                      0.0,
                                      m_density,
                                      m_machNumber*m_speedOfSound,
                                      1.0, 
                                      1.0,
                                      isInviscid);
    }
}