/*
 * The purpose of this class is to handle any operation related to running 
 * either the primal or adjoint solver. 
 */

package framework;

import star.common.*;
import star.coupledflow.*;
import star.flow.*;
import star.kwturb.KOmegaTurbulence;
import star.kwturb.KwAllYplusWallTreatment;
import star.kwturb.SstKwTurbModel;
import star.material.SingleComponentGasModel;
import star.turbulence.RansTurbulenceModel;
import star.turbulence.TurbulentModel;

import java.util.Collection;

/**
 *
 * @author shb
 */
public class Solver extends Toolbox {
    
    // --- Properties 
    // m_<name> = member variable (only accessible inside class)
    final private Toolbox m_toolbox;    
    final private Simulation m_simulation;

    private boolean m_isAdjoint; 
    private String m_fluidRegionName; 
    private String m_physicsContinuumName; 
    private double m_primalCFL;
    private double m_adjointCFL;
    private double m_referencePressure; 
    
    // --- Constructor 
    public Solver(Simulation sim) {
        super(sim); 
        m_toolbox = new Toolbox(sim); 
        m_simulation = sim; 
        m_fluidRegionName = "Fluid"; 
        m_physicsContinuumName = "Physics"; 
        m_primalCFL = 5.0; 
        m_adjointCFL = 5.0; 
        m_referencePressure = 101325; // Pa
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    public void setPrimalCFL(double d){m_primalCFL = d;}
    public void setAdjointCFL(double d){m_adjointCFL = d;}
    
    public double getPrimalCFL(){return m_primalCFL;}
    public double getAdjointCFL(){return m_adjointCFL;}
    
    // This method clears the previous setup
    public void clear(){
        ContinuumManager continuumManager = m_simulation.getContinuumManager(); 
        RegionManager regionManager = m_simulation.getRegionManager();
        if (regionManager.has(m_fluidRegionName)){
            Region region = regionManager.getObject(m_fluidRegionName);
            if (continuumManager.has(m_physicsContinuumName)){
                PhysicsContinuum physicsContinuum = ((PhysicsContinuum) continuumManager.getContinuum(m_physicsContinuumName));
                if (physicsContinuum.hasRegion(region)){
                    physicsContinuum.erase(region); 
                }
            }
        }
        m_toolbox.deletePhysicsContinuum(m_physicsContinuumName);  
    }
   
    // This method selects the physics models and sets reference values.
    public void createPhysicsContinuum(String continuumName, boolean isInviscid, double referencePressure){
        m_physicsContinuumName = continuumName;
        m_referencePressure = referencePressure;
        PhysicsContinuum physicsContinuum = m_simulation.getContinuumManager().createContinuum(PhysicsContinuum.class);
        physicsContinuum.setPresentationName(m_physicsContinuumName);
        physicsContinuum.enable(SteadyModel.class);
        physicsContinuum.enable(SingleComponentGasModel.class);
        physicsContinuum.enable(CoupledFlowModel.class);
        physicsContinuum.enable(IdealGasModel.class);
        physicsContinuum.enable(CoupledEnergyModel.class);
        if (isInviscid){
            physicsContinuum.enable(InviscidModel.class);
        }
        else {
            physicsContinuum.enable(TurbulentModel.class);
            physicsContinuum.enable(RansTurbulenceModel.class);
            physicsContinuum.enable(KOmegaTurbulence.class);
            physicsContinuum.enable(SstKwTurbModel.class);
            physicsContinuum.enable(KwAllYplusWallTreatment.class);
        }
        physicsContinuum.getReferenceValues().get(ReferencePressure.class).setValue(m_referencePressure);
    }    
    
    // This method adds allocates a region on which the physics are to be solve
    public void addRegionToPhysics(String regionName){
        m_fluidRegionName = regionName;
        Region region = m_simulation.getRegionManager().getRegion(m_fluidRegionName);
        Continuum physicsContinuum = m_simulation.getContinuumManager().getContinuum(m_physicsContinuumName);
        physicsContinuum.add(region);
    }
    
    // This method creates a primal solver
    public void createPrimalSolver(double CFL){
        m_primalCFL = CFL; 
        CoupledImplicitSolver coupledImplicitSolver = ((CoupledImplicitSolver) m_simulation.getSolverManager().getSolver(CoupledImplicitSolver.class));
        coupledImplicitSolver.setCFL(m_primalCFL);
    }
    
    // This method creates an adjoint solver
    public void createAdjointSolver(double CFL){
        m_adjointCFL = CFL; 
        m_toolbox.activateAdjointSolver(m_physicsContinuumName);
        SolverManager solverManager = m_simulation.getSolverManager(); 
        AdjointFlowSolver adjointFlowSolver = solverManager.getSolver(AdjointFlowSolver.class); 
        adjointFlowSolver.setAdjointCFL(m_adjointCFL);
    }
    
    // This method sets boundary conditions 
    public void setBoundaryConditionType(String boundaryName, String type){
        switch (type){
            case "Freestream": 
                m_toolbox.setBoundaryConditionFreestream(m_fluidRegionName,boundaryName); 
                break; 
            case "Wall": 
                m_toolbox.setBoundaryConditionWall(m_fluidRegionName,boundaryName); 
                break; 
            case "Symmetry": 
                m_toolbox.setBoundaryConditionSymmetry(m_fluidRegionName,boundaryName); 
                break; 
        }
    }
    
    // This method sets flight conditions 
    public void setFreestreamFlightCondition(String type, String boundaryName, double[] value){
        switch (type){
            case "Mach Number":
                m_toolbox.setFreestreamMachNumber(m_fluidRegionName,
                                                  boundaryName,value[0]);
                break; 
            case "Altitude":
                m_toolbox.setFreestreamAltitude(m_fluidRegionName,
                                                  boundaryName,value[0]);
                break; 
            case "Velocity":
                m_toolbox.setFreestreamVelocity(m_physicsContinuumName,
                                                m_fluidRegionName,
                                                boundaryName,value);
                break; 
            case "Pressure":
                m_toolbox.setFreestreamPressure(m_physicsContinuumName,
                                                m_fluidRegionName,
                                                boundaryName,value[0]);
                break; 
            case "Temperature":
                m_toolbox.setFreestreamTemperature(m_physicsContinuumName,
                                                   m_fluidRegionName,
                                                   boundaryName,value[0]);
                break; 
            case "Dynamic Viscosity":
                m_toolbox.setFreestreamDynamicViscosity(m_physicsContinuumName,
                                                        value[0]);
                break; 
        }
    }
    // ********************** overloading **************************************
    public void setFreestreamFlightCondition(String type, String boundaryName, double scalarValue){
        double[] arrayValue = {scalarValue}; 
        setFreestreamFlightCondition(type,boundaryName,arrayValue);
    }
    // *************************************************************************
   
    // This method runs the primal solver
    public void runPrimal(int numberSteps){
        CoupledImplicitSolver coupledImplicitSolver = ((CoupledImplicitSolver) m_simulation.getSolverManager().getSolver(CoupledImplicitSolver.class));
        coupledImplicitSolver.setCFL(m_primalCFL);
        increaseMaxSteps(numberSteps);
        enableMonitorIterationStoppingCriterion(true);
        m_simulation.getSimulationIterator().run();
    } 
    
    // This method clear the residual histories and restarts the primal solver
    public void restartPrimal(int numberSteps){
        clearHistories();
        enableMonitorIterationStoppingCriterion(true);
        runPrimal(numberSteps);
    } 
    
    // This method runs the adjoint solver
    public void runAdjoint(int numberSteps){runAdjointWithWarmStart(0, numberSteps);}
    public void runAdjointWithWarmStart(int number1stOrderGMRESSteps, int number2ndOrderGMRESSteps){
        // Disable primal solver stopping criterion (otherwise adjoint won't run)
        enableMonitorIterationStoppingCriterion(false);

        // Get adjoint solver 
        SolverManager solverManager = m_simulation.getSolverManager(); 
        AdjointFlowSolver adjointFlowSolver = solverManager.getSolver(AdjointFlowSolver.class); 
        AdjointRunnableSolver adjointRunnableSolver = adjointFlowSolver.getAdjointRunnableSolver();
        AdjointMeshSolver adjointMeshSolver = m_simulation.getSolverManager().getSolver(AdjointMeshSolver.class);
        
        // Unfreeze solver 
        adjointRunnableSolver.setFrozen(false);

        // Clear previous adjoint solution (if any)
        clearAdjoint();

        // Get objects
        PhysicsContinuum physicsContinuum_0 = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(m_physicsContinuumName));
        AdjointFlowModel adjointFlowModel_0 =  physicsContinuum_0.getModelManager().getModel(AdjointFlowModel.class);
        AdjointFlowSolver adjointFlowSolver_0 = ((AdjointFlowSolver) m_simulation.getSolverManager().getSolver(AdjointFlowSolver.class));

        // Set Courant number
        adjointFlowSolver.setAdjointCFL(m_adjointCFL);

        // Run adjoint
        if (number1stOrderGMRESSteps == 0 || number2ndOrderGMRESSteps == 0){
            int numberSteps = Math.max(number1stOrderGMRESSteps, number2ndOrderGMRESSteps);
            increaseMaxSteps(numberSteps);
            m_simulation.getSimulationIterator().run(adjointRunnableSolver);
        }
        else {

            // Use GMRES for difficult to converge simulations
            adjointFlowSolver_0.getAccelerationOption().setSelected(AdjointAccelerationOption.Type.RESTARTED_GMRES);

            // Warm-start using 1st-order scheme
            adjointFlowModel_0.getUpwindOption().setSelected(FlowUpwindOption.Type.FIRST_ORDER);
            increaseMaxSteps(number1stOrderGMRESSteps);
            m_simulation.getSimulationIterator().run(adjointRunnableSolver);

            // Run using 2nd-order scheme
            adjointFlowModel_0.getUpwindOption().setSelected(FlowUpwindOption.Type.SECOND_ORDER);
            increaseMaxSteps(number2ndOrderGMRESSteps);
            m_simulation.getSimulationIterator().run(adjointRunnableSolver);
        }

        // Compute sensitivities
        adjointMeshSolver.computeMeshSensitivity();
        adjointFlowSolver.computeAdjointErrorEstimates();
        
        // Freeze
        adjointRunnableSolver.setFrozen(true);
    } 
    
    // This method clears the current solution, but does not reset the mesh
    public void clearSolutionButDoNotResetMesh(){
        Solution solution = m_simulation.getSolution(); 
        solution.clearSolution(Solution.Clear.History, 
                               Solution.Clear.Fields, 
                               Solution.Clear.AdjointFlow);
    }
    
    // This method clears histories but not the solution
    public void clearHistories(){
        Solution solution = m_simulation.getSolution();
        solution.clearSolution(Solution.Clear.History);
    }

    // This method clears the adjoint solution
    public void clearAdjoint(){
        Solution solution = m_simulation.getSolution();
        solution.clearSolution(Solution.Clear.AdjointFlow);
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------

    // This method enables or disables monitor stopping criterion
    private void enableMonitorIterationStoppingCriterion(boolean flag) {
        SolverStoppingCriterionManager manager = m_simulation.getSolverStoppingCriterionManager();
        Collection<SolverStoppingCriterion> children = manager.getObjects();
        for (SolverStoppingCriterion child : children) {
            if (child.getClass().equals(MonitorIterationStoppingCriterion.class)) {
                child.setIsUsed(flag);
            }
        }
    }

    // This method increases the maximum number of solver steps
    private void increaseMaxSteps(int numberSteps){
        
        SolverStoppingCriterionManager solverStoppingCriterionManager; 
        SolverStoppingCriterion solverStoppingCriterion;
        StepStoppingCriterion stepStoppingCriterion; 
        SimulationIterator simulationIterator; 
        
        solverStoppingCriterionManager = m_simulation.getSolverStoppingCriterionManager(); 
        solverStoppingCriterion = solverStoppingCriterionManager.getSolverStoppingCriterion("Maximum Steps"); 
        stepStoppingCriterion = (StepStoppingCriterion) solverStoppingCriterion;
        simulationIterator = m_simulation.getSimulationIterator(); 
        
        int currentSteps = simulationIterator.getCurrentIteration();
        int maxSteps = numberSteps + currentSteps;
        stepStoppingCriterion.setMaximumNumberSteps(maxSteps);
    }
}
