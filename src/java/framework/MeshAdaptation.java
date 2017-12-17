/*
 * This class provides methods to adaptively refine a mesh, assuming the CFD 
 * simulation is already setup to the point that the mesh can be generated 
 * and the primal and adjoint solvers ran. Use cases: 
 * 
 * 1) You have a simulation read to go, but want to adapt the mesh w.r.t. a  
 *    single adjoint cost function (e.g. CD or CL) 
 * 
 * The method use for adaptive mesh refinement is described in: 
 * Venditti et al., "Grid Adaptation for Functional Outputs: Application to 2D 
 * Inviscid Flows," Journal of Computational Physics, 2002, Vol 176, pp. 40-69 
 */

package framework;

import star.base.report.ElementCountReport;
import star.common.Simulation;
import star.common.XyzInternalTable;
import star.dualmesher.DualAutoMesher;
import star.meshing.AutoMeshOperation;
import star.meshing.MeshOperationManager;
import star.resurfacer.ResurfacerAutoMesher;
import star.trimmer.TrimmerAutoMesher;
import star.twodmesher.DualAutoMesher2d;

import java.io.File;

/**
 *
 * @author shb
 */
public class MeshAdaptation {
    
    // -------------------------------------------------------------------------
    // ---------------------- A T T R I B U T E S ------------------------------
    // -------------------------------------------------------------------------
    
    public Simulation simulation;
    public Mesher mesher; 
    public Solver solver;
    public Toolbox toolbox;
    
    private String m_meshOperationName;
    private String m_meshRefinementTableName; 
    private String m_adaptiveMeshCostFunctionName;
    private String m_fluidRegionName;
    private String m_adaptiveMeshCostFunctionReportName;
    private double m_adaptTargetError;
    private double m_adaptMaxSizeChange;
    private String epsilon_k;
    private String H_k;
    private String eta_g;
    private String eta_k;
    private String H_k_tilde;
    private String epsilon_0;
    private String epsilon_0_bar;
    private String refinementFactor;
    private String omega;
    
    // -------------------------------------------------------------------------
    // ---------------------- C O N S T R U C T O R ----------------------------
    // -------------------------------------------------------------------------
    
    public MeshAdaptation(Simulation sim)  {
        // - Objects -
        simulation = sim; 
        toolbox = new Toolbox(sim);
        mesher = new Mesher(sim); 
        solver = new Solver(sim); 
        
        // - Names -
        m_meshOperationName = "Automated Mesh";
        m_meshRefinementTableName = "Mesh Refinement"; 
        m_adaptiveMeshCostFunctionName = "CD";
        m_fluidRegionName = "Fluid";
        m_adaptiveMeshCostFunctionReportName = "CD";
        // - Mesh Adaptation -
        m_adaptTargetError = 0.0005; // Error per cell. How accurate do you want the result? e.g. 5 drag counts = 0.0005
        m_adaptMaxSizeChange = 2.0; // Allowed size change between two consecutive refinement levels

        // Parameters in paper by Venditti et al. (2002). Refer to section 3.2.
        epsilon_k = "AbsError"; // Estimated error at element k in eq. 16
        H_k = "CellSize"; // Current element size in eq. 20
        eta_g = "GlobalAdapt"; // Global adaptation parameter in eq. 20
        eta_k = "LocalAdapt"; // Local adaptation parameter at element k in eq. 20
        H_k_tilde = "NewSize"; // New element size after refinement in eq. 20
        epsilon_0 = "TargetError"; // User-specified desired error level  in eq. 18
        epsilon_0_bar = "Avg_Error"; // average target error in eq. 19
        refinementFactor = "Refinement"; // Fraction by which to shrink or grow element "(...)^(1/4)" in eq. 20
        omega = "0.25"; // exponent in eq. 20
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    
    public void setPrimalCFL(double d){solver.setPrimalCFL(d);}
    public void setAdjointCFL(double d){solver.setAdjointCFL(d);}
    public void setFluidRegionName(String s){m_fluidRegionName = s;}
    public void setMeshRefinementTableName(String s){m_meshRefinementTableName = s;}
    public void setAdaptiveMeshCostFunctionName(String s){m_adaptiveMeshCostFunctionName = s;}
    public void setAdaptiveMeshCostFunctionReportName(String s){m_adaptiveMeshCostFunctionReportName = s;}
    public void setMeshOperationNameName(String s){m_meshOperationName = s;}
    public void setAdaptTargetError(double d){m_adaptTargetError = d;}
    public void setAdaptMaxSizeChange(double d){m_adaptMaxSizeChange = d;}
    
    public double getPrimalCFL(){return solver.getPrimalCFL();}
    public double getAdjointCFL(){return solver.getAdjointCFL();}
    public String getFluidRegionName(){return m_fluidRegionName;}
    public String getMeshRefinementName(){return m_meshRefinementTableName;}
    public String getAdaptiveMeshCostFunctionName(){return m_adaptiveMeshCostFunctionName;}
    public String getAdaptiveMeshCostFunctionReportName(){return m_adaptiveMeshCostFunctionReportName;}
    public String getMeshOperationNameName(){return m_meshOperationName;}
    public double getAdaptTargetError(){return m_adaptTargetError;}
    public double getAdaptMaxSizeChange(){return m_adaptMaxSizeChange;}
    
    // This method performs adaptive mesh refinement
    public void runWithWarmStart(int numberAdaptionLevels,
                                 int numberPrimalSteps,
                                 int number1stOrderGMRESAdjointSteps,int number2ndOrderGMRESAdjointSteps){
        clearAdaptiveMeshRefinementSetup();
        mesher.mesh();
        prepareForAdaptiveMeshRefinement();
        solver.restartPrimal(numberPrimalSteps);
        solver.runAdjointWithWarmStart(number1stOrderGMRESAdjointSteps, number2ndOrderGMRESAdjointSteps);
//        simulation.saveState(simulation.getSessionPath());
        String simulationName = simulation.getPresentationName();
        simulation.saveState(simulation.getSessionDir() + File.separator + simulationName + "_Initial.sim");
        for (int count = 1; count <= numberAdaptionLevels; count++) {
            simulation.println(" ");
            simulation.println("***********************");
            simulation.println("**ADAPTATION LEVEL " + count + " **");
            simulation.println("***********************");
            simulation.println(" ");
            simulation.getTableManager().getTable(m_meshRefinementTableName).extract();
            mesher.mesh();
            solver.restartPrimal(numberPrimalSteps);
            solver.runAdjointWithWarmStart(number1stOrderGMRESAdjointSteps, number2ndOrderGMRESAdjointSteps);
//            simulation.saveState(simulation.getSessionPath());
            simulation.saveState(simulation.getSessionDir() + File.separator + simulationName + "_Adapted.sim");
        }
        clearAdaptiveMeshRefinementSetup();
    }
    public void run(int numberAdaptionLevels,int numberPrimalSteps,int numberAdjointSteps){ 
        clearAdaptiveMeshRefinementSetup(); 
        mesher.mesh();
        prepareForAdaptiveMeshRefinement();
        solver.restartPrimal(numberPrimalSteps);
        solver.runAdjoint(numberAdjointSteps);
        for (int count = 1; count <= numberAdaptionLevels; count++) {
            simulation.getTableManager().getTable(m_meshRefinementTableName).extract();
            mesher.mesh();
            solver.restartPrimal(numberPrimalSteps);
            solver.runAdjoint(numberAdjointSteps);

        }
        clearAdaptiveMeshRefinementSetup();
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // This method prepares for 2D adaptive mesh refinement. Specifically, 
    // it creates a threshold derived part to exclude the prism layer
    // from refinement (if present), creates the field functions necessary to
    // compute cell refinement throughout the domain, and creates a mesh refine-
    // ment table which is attached to the mesher. The method use for adaptive
    // mesh refinement is described in: Venditti et al., "Grid Adaptation for 
    // Functional Outputs: Application to 2D Inviscid Flows," Journal of Comput-
    // ational Physics, 2002, Vol 176, pp. 40-69 
    private void prepareForAdaptiveMeshRefinement(){

        // --------------------- get mesher ---------------------------------------

        // Instantiate an object for all mesher classes. If the "hasObjectOfClass" method do not
        // find an object of the specified class, the object instance will be of type null.

        AutoMeshOperation autoMeshOperation_0 =
                ((AutoMeshOperation) simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));

        boolean isMesh2D = autoMeshOperation_0.is2dOperation();

        DualAutoMesher2d dualAutoMesher2d_0 = ((DualAutoMesher2d) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.twodmesher.DualAutoMesher2d"));

        ResurfacerAutoMesher resurfacerAutoMesher_0 = ((ResurfacerAutoMesher) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.resurfacer.ResurfacerAutoMesher"));

        DualAutoMesher dualAutoMesher_0 = ((DualAutoMesher) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.dualmesher.DualAutoMesher"));

        TrimmerAutoMesher trimmerAutoMesher_0 = ((TrimmerAutoMesher) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.trimmer.TrimmerAutoMesher"));

        // --------------------- Setup -----------------------------------------
        
        // Starccm+ assigns a cost function ID each time it is created. This ID
        // is necessary in order to retrieve field function information for a 
        // given cost function. So let's retrieve that ID. 
        int costFunctionID = toolbox.getCostFunctionID(m_adaptiveMeshCostFunctionName); 

        // The following reports will be create, so first we pick the name
        String costFunctionErrorReportName = m_adaptiveMeshCostFunctionName+"_Error";
        String elementCountReportName = "Element_Count";

        // The following field function will be used 
        toolbox.createElementCountReport(elementCountReportName, m_fluidRegionName);

        // The following field function will be used
        String costFunctionErrorFieldFunctionName = "Adjoint"+costFunctionID+"::AdjointErrorEstimate";

        // Similarly, a derived threshold part will be created, in order to 
        // filter out prism layers from mesh refinement. Here we pick the name 
        // of this part. 
        String derivedPartName = "Threshold"; 
        
        // Setup field functions for method in the paper by Venditti et al. (2002)
        String fieldFunctionName1 = epsilon_k;
        String fieldFunctionName2 = H_k;
        String fieldFunctionName3 = eta_g;
        String fieldFunctionName4 = eta_k;
        String fieldFunctionName5 = H_k_tilde;
        String fieldFunctionName6 = refinementFactor;
        String fieldFunctionName7 = epsilon_0;
        String expression1 = "abs(${Adjoint"+costFunctionID+"::AdjointErrorEstimate})";
        String expression2;
        if (isMesh2D){expression2 = "1.2*pow(${Volume},0.5)";}
        else if (dualAutoMesher_0 != null) {expression2 = "1.2*pow(${Volume},1/3)";}
        else {expression2 = "pow(${Volume},1/3)";}
        String expression3 = "abs(${"+costFunctionErrorReportName+"Report})/${"+epsilon_0+"}";
        String expression4 = "max(${"+epsilon_k+"}/${"+epsilon_0_bar+"Report},1e-3)";
        String expression5 = "${"+H_k+"}*${"+refinementFactor+"}";
        String expression6 = "(${PrismLayerCells}>0.5)? 1: max(min(pow(1/" +
                             "(${"+eta_g+"}*${"+eta_k+"}),"+omega+"),"+m_adaptMaxSizeChange+")," +
                             ""+(1/m_adaptMaxSizeChange)+")";
        String expression7 = String.valueOf(m_adaptTargetError);

        // --------------------- creation --------------------------------------
        
        // First, let's create the field functions
        toolbox.createUserFieldFunction(fieldFunctionName1,expression1);
        toolbox.createUserFieldFunction(fieldFunctionName2,expression2);
        toolbox.createUserFieldFunction(fieldFunctionName3,expression3);
        toolbox.createUserFieldFunction(fieldFunctionName4,expression4);
        toolbox.createUserFieldFunction(fieldFunctionName5,expression5);
        toolbox.createUserFieldFunction(fieldFunctionName6,expression6);
        toolbox.createUserFieldFunction(fieldFunctionName7,expression7);
        
        // Second, we create a threshold part to exclude prism layers
        toolbox.createDerivedPartThreshold(derivedPartName,"PrismLayerCells",m_fluidRegionName); 
        
        // Third, we create a sum report to for the adjoint error estimate
        toolbox.createSumReport(costFunctionErrorReportName,derivedPartName,costFunctionErrorFieldFunctionName);
        
        // Fourth, we create a volume report to compute the average error 
        toolbox.createVolumeReport(epsilon_0_bar,derivedPartName,epsilon_k);

        // Sixth, we create a mesh refinement table
        toolbox.createXyzTable(m_meshRefinementTableName,derivedPartName,fieldFunctionName5); 

        // --------------------- link table ---------------------------------------
        
        // Finally, we connect the mesh refinement table to the polygonal mesher  
        XyzInternalTable refinementTable =
                ((XyzInternalTable) simulation.getTableManager().getTable(m_meshRefinementTableName));

        // We assign the mesh refinement table to all mesher previously instantiated. The null
        // check prevent a NullPointerException in case the mesher does not exist.

        if (dualAutoMesher2d_0 != null) {
            toolbox.print("Mesh is 2D...");
            dualAutoMesher2d_0.setMeshSizeTable(refinementTable);
        }
        if (resurfacerAutoMesher_0 != null) {
            resurfacerAutoMesher_0.setMeshSizeTable(refinementTable);
        }
        if (dualAutoMesher_0 != null) {
            toolbox.print("Mesh is Polyhedral...");
            dualAutoMesher_0.setMeshSizeTable(refinementTable);
        }
        if (trimmerAutoMesher_0 != null) {
            toolbox.print("Mesh is Trimmer...");
            trimmerAutoMesher_0.setMeshSizeTable(refinementTable);
        }

    }
    
    // This method clears the adaptive mesh refinement settings. 
    private void clearAdaptiveMeshRefinementSetup(){

        // Instantiate an object for all mesher classes. If the "hasObjectOfClass" method do not
        // find an object of the specified class, the object instance will be of type null.

        AutoMeshOperation autoMeshOperation_0 =
                ((AutoMeshOperation) simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));

        DualAutoMesher2d dualAutoMesher2d_0 = ((DualAutoMesher2d) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.twodmesher.DualAutoMesher2d"));

        ResurfacerAutoMesher resurfacerAutoMesher_0 = ((ResurfacerAutoMesher) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.resurfacer.ResurfacerAutoMesher"));

        DualAutoMesher dualAutoMesher_0 = ((DualAutoMesher) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.dualmesher.DualAutoMesher"));

        TrimmerAutoMesher trimmerAutoMesher_0 = ((TrimmerAutoMesher) autoMeshOperation_0
                .getMeshers().hasObjectOfClass("star.trimmer.TrimmerAutoMesher"));

        // We remove any pointer to the refinement table. Assigning the value null to
        // "setMeshSizeTable" is equivalent to selecting "None" in the GUI. The null check
        // prevent a NullPointerException in case the mesher does not exist.

        if (dualAutoMesher2d_0 != null) {
            dualAutoMesher2d_0.setMeshSizeTable(null);
        }
        if (resurfacerAutoMesher_0 != null) {
            resurfacerAutoMesher_0.setMeshSizeTable(null);
        }
        if (dualAutoMesher_0 != null) {
            dualAutoMesher_0.setMeshSizeTable(null);
        }
        if (trimmerAutoMesher_0 != null) {
            trimmerAutoMesher_0.setMeshSizeTable(null);
        }

        // Variables as named in prepareForAdaptiveMeshRefinement() 
        String derivedPartName = "Threshold";

        String fieldFunctionName1 = epsilon_k;
        String fieldFunctionName2 = H_k;
        String fieldFunctionName3 = eta_g;
        String fieldFunctionName4 = eta_k;
        String fieldFunctionName5 = H_k_tilde;
        String fieldFunctionName6 = refinementFactor;
        String fieldFunctionName7 = epsilon_0;
        
        String costFunctionErrorReportName = m_adaptiveMeshCostFunctionName+"_Error"; 
        String averageErrorReportName = "Avg_Error";
        String elementCountReportName = "Element_Count";

        
        // Now, let's delete any previous runs 
        toolbox.deleteTable(m_meshRefinementTableName);

        toolbox.deleteUserFieldFunction(fieldFunctionName5); 
        toolbox.deleteUserFieldFunction(fieldFunctionName2); 
        toolbox.deleteUserFieldFunction(fieldFunctionName6);
        toolbox.deleteUserFieldFunction(fieldFunctionName3);
        toolbox.deleteUserFieldFunction(fieldFunctionName4);
        toolbox.deleteUserFieldFunction(fieldFunctionName7);

        toolbox.deleteReport(costFunctionErrorReportName);
        toolbox.deleteReport(averageErrorReportName);
        toolbox.deleteReport(elementCountReportName);

        toolbox.deleteUserFieldFunction(fieldFunctionName1);

        toolbox.deleteDerivedPart(derivedPartName);
    }
}
