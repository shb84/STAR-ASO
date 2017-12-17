/*
 * The purpose of this class is to provide common utility functions to subclasses.
 */

package framework;

import star.common.*;

import java.io.File;
import java.util.ArrayList;
/**
 *
 * @author shb
 */
public class CFDModel implements Blackbox {
    
    // -------------------------------------------------------------------------
    // ---------------------- A T T R I B U T E S ------------------------------
    // -------------------------------------------------------------------------
    
    // - Names - 
    private String m_freestreamBoundaryName;
    private String m_fluidRegionName; 
    
    // - Objects - 
    private final Simulation simulation;
    public final Solver solver;
    public final Mesher mesher;
    private final Morpher morpher; 
    private final Toolbox toolbox;
    public FlightCondition flightCondition;

    // - Flags -
    private boolean m_isFlow2D;

    // -------------------------------------------------------------------------
    // ---------------------- C O N S T R U C T O R ----------------------------
    // -------------------------------------------------------------------------
    
    public CFDModel(Simulation sim) {
        // - Objects -
        simulation = sim;
        toolbox = new Toolbox(sim);
        mesher = new Mesher(sim);
        solver = new Solver(sim);  
        morpher = new Morpher(sim); 
        flightCondition = new FlightCondition(sim);
        // - Names - 
        m_freestreamBoundaryName = "Farfield"; 
        m_fluidRegionName = "Fluid";
        // - Flags -
        m_isFlow2D = false;
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    // User settings
    public void setFreestreamBoundaryName(String s){m_freestreamBoundaryName = s;}
    public void setFluidRegionName(String s){m_fluidRegionName = s;}
    public void set2DFlag(boolean b){
        m_isFlow2D = b;
        flightCondition.set2DFlag(b);
    }
    
    // This method sets the angle of attack
    public void setAngleOfAttack(double angle){flightCondition.setAngleOfAttack(angle);}
    
    // This method returns the function corresponding to the function name 
    public double getFunctionValue(String functionName){return toolbox.getReportValue(functionName);
    }

    // This method returns the partial corresponding to the function name 
    public double getPartialDerivative(String functionName, DesignVariable designVariable){
        // There can be two types of design parameters: control points and 
        // boundary parameters (e.g. alpha). The are handled differently: 
        if (designVariable.getVarName().equals("alpha")){
            return getAlphaDerivative(functionName);
        }
        else {
            int row = designVariable.getControlPointTableRow(); 
            int col = designVariable.getControlPointTableCol()-3; 
            String controlPointTableName = designVariable.getControlPointTableID(); 
            TableData tableData = toolbox.getGradientTableData(functionName,controlPointTableName);
            return tableData.getSeries(col)[row]; 
        }
    }  
    
    // This method updates the control points 
    public void updateControlPoints(ArrayList<DesignVariable> designVariables){
        morpher.update(designVariables);
        morpher.morph();
    }
    
    // This method runs the primal solver
    public void runPrimalSolver(int numberOfSteps){solver.runPrimal(numberOfSteps);}
    
    // This method runs the adjoint solver
    public void runAdjointSolver(int numberOfSteps){solver.runAdjoint(numberOfSteps);}
    
    // This method saves the simulation
    public void save(String simName){
        simulation.saveState(simulation.getSessionDir() + File.separator + simName);
    }
    public void save(){simulation.saveState(simulation.getSessionPath());}
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // This method computes the alpha partial derivative at alpha = a0: 
    // df_dalpha = df_ddx * ddx_dalpha + df_ddy * ddy_dalpha 
    //           = df_ddx * (-sin(a0)) + df_ddy * ( cos(a0)) 
    // dx = cos(alpha) <---- flow direction along x 
    // dy = sin(alpha) <---- flow direction along y 
    // 
    // N.B. the adjoint w.r.t. alpha requires 2 "boundary parameter sensitivity"
    // reports, which is only available in Star-CCM+ version 11.06.XXX onward: 
    // "d" + functionName + "d_dX" (e.g. dCL_dX) <----- sensitivity w.r.t. dx
    // "d" + functionName + "d_dY" (e.g. dCL_dY) <----- sensitivity w.r.t. dy
    private double getAlphaDerivative(String functionName){
        // Create boundary parameter sensitivity reports if they don't exist
        toolbox.createBoundaryParameterSensitivityReport(
                                            m_fluidRegionName,
                                            m_freestreamBoundaryName,functionName,
                                            "FLOW_DIRECTION_X",
                                            "Gradient of " + functionName + " w.r.t. Flow Direction[0]");

        if (m_isFlow2D) {
            toolbox.createBoundaryParameterSensitivityReport(
                    m_fluidRegionName,
                    m_freestreamBoundaryName, functionName,
                    "FLOW_DIRECTION_Y",
                    "Gradient of " + functionName + " w.r.t. Flow Direction[1]");
            toolbox.createForceExpressionReport("Gradient of " + functionName + " w.r.t. angle-of-attack",
                    "(3.14159/180)*(${Gradientof" + functionName + "w.r.t.FlowDirection[0]Report}*(-sin($alpha)) + ${Gradientof" + functionName + "w.r.t.FlowDirection[1]Report}*cos($alpha))");
        }
        else {
            toolbox.createBoundaryParameterSensitivityReport(
                    m_fluidRegionName,
                    m_freestreamBoundaryName, functionName,
                    "FLOW_DIRECTION_Z",
                    "Gradient of " + functionName + " w.r.t. Flow Direction[2]");
            toolbox.createForceExpressionReport("Gradient of " + functionName + " w.r.t. angle-of-attack",
                    "(3.14159/180)*(${Gradientof" + functionName + "w.r.t.FlowDirection[0]Report}*(-sin($alpha)) + ${Gradientof" + functionName + "w.r.t.FlowDirection[2]Report}*cos($alpha))");
        }
        double df_dalpha = toolbox.getReportValue("Gradient of " + functionName + " w.r.t. angle-of-attack"); 
        return df_dalpha;  
    }
}