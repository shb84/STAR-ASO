/*
 * This is the interface to the Problem class. 
 */

package framework;

import java.util.ArrayList;

/**
 *
 * @author shb
 */
public interface Blackbox {
    
    void setAngleOfAttack(double angle);
    double getFunctionValue(String functionName);
    double getPartialDerivative(String functionName, DesignVariable designVariable);
    void runPrimalSolver(int numberOfSteps); 
    void runAdjointSolver(int numberOfSteps);
    void updateControlPoints(ArrayList<DesignVariable> designVariables); 
}
