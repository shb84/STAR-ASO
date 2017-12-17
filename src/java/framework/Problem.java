/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package framework;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author shb
 */
public class Problem {
    
    // Design variables
    public ArrayList<DesignVariable> designVariables;
    private int m_numDesignVariables; 

    // Objective functions
    private ArrayList<Double> m_objectiveFunctionValue; 
    private ArrayList<double[]> m_objectiveFunctionGradient; 
    private ArrayList<String> m_objectiveFunctionName;
    private int m_numObjectiveFunctions; 

    // Inequality constraints
    private ArrayList<Double> m_inequalityConstraintValue; 
    private ArrayList<double[]> m_inequalityConstraintGradient; 
    private ArrayList<String> m_inequalityConstraintName; 
    private int m_numInequalityConstraints; 

    // Equality constraints 
    private ArrayList<Double> m_equalityConstraintValue; 
    private ArrayList<double[]> m_equalityConstraintGradient; 
    private ArrayList<String> m_equalityConstraintName; 
    private int m_numEqualityConstraints; 

    // Function evaluation 
    public Blackbox cfd; 

    // Independent variable information
    private String m_varnameHeader; 
    private String m_xvalHeader;  
    private String m_xinitialHeader; 
    private String m_xminHeader;  
    private String m_xmaxHeader; 
    private String m_typicalXHeader; 
    private String m_controlPointTableIDHeader; 
    private String m_controlPointTableRowHeader; 
    private String m_controlPointTableColHeader; 

    // Dependent variable information
    private String m_functionTypeHeader;  
    private String m_functionNameHeader; 
    private String m_functionValueHeader; 
    private String m_partialPrefixHeader; 

    // Function and gradient evaluation counters
    
    // --- Constructor 
    public Problem(Blackbox blackbox){
        
        this.cfd = blackbox; 
        
        // Independent variable file
        m_varnameHeader = "VarName"; 
        m_xvalHeader = "X"; 
        m_xinitialHeader = "X0"; 
        m_xminHeader = "Xmin"; 
        m_xmaxHeader = "Xmax"; 
        m_typicalXHeader = "TypicalX"; 
        m_controlPointTableIDHeader = "ControlPointTableID"; 
        m_controlPointTableRowHeader = "ControlPointTableRow"; 
        m_controlPointTableColHeader = "ControlPointTableCol"; 
        
        // Dependent variable file
        m_functionTypeHeader = "Type";  
        m_functionNameHeader = "Name";  
        m_functionValueHeader = "F"; 
        m_partialPrefixHeader = "dFd";
        
        // Design variables
        designVariables = new ArrayList();
        m_numDesignVariables = 0; 
        
        // Objective functions
        m_objectiveFunctionValue = new ArrayList();
        m_objectiveFunctionGradient = new ArrayList();
        m_objectiveFunctionName = new ArrayList(); 
        m_numObjectiveFunctions = 0; 
        
        // Inequality constraints
        m_inequalityConstraintValue = new ArrayList();
        m_inequalityConstraintGradient = new ArrayList();
        m_inequalityConstraintName = new ArrayList();  
        m_numInequalityConstraints = 0; 
        
        // Equality constraints
        m_equalityConstraintValue = new ArrayList(); 
        m_equalityConstraintGradient = new ArrayList();
        m_equalityConstraintName = new ArrayList(); 
        m_numEqualityConstraints = 0;  
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------

    // This method reads independent variables from a CSV file 
    public void readIndependentVariables(String CSVfilepath) {
       
        // Extract data from CSV file
        CSVFile  csv  = new CSVFile(CSVfilepath); 
        try {
            String[] name = csv.getColumn(m_varnameHeader);
            String[] xval = csv.getColumn(m_xvalHeader);
            String[] x0   = csv.getColumn(m_xinitialHeader);
            String[] xmin = csv.getColumn(m_xminHeader);
            String[] xmax = csv.getColumn(m_xmaxHeader);
            String[] typx = csv.getColumn(m_typicalXHeader);
            String[] tabl = csv.getColumn(m_controlPointTableIDHeader);
            String[] rows = csv.getColumn(m_controlPointTableRowHeader);
            String[] cols = csv.getColumn(m_controlPointTableColHeader);

            // Loop through inputs 
            for (int i = 0; i < tabl.length; i++){
                
                // Update boundary parameters
                if (name[i].equals("alpha")){
                    cfd.setAngleOfAttack(Double.parseDouble(xval[i]));
                }
                // place holder for other boundary parameters 
                
                // Populate design variable information
                DesignVariable x = new DesignVariable(  tabl[i],
                                                        name[i],
                                                        Integer.parseInt(rows[i]),
                                                        Integer.parseInt(cols[i]), 
                                                        Double.parseDouble(xval[i]),
                                                        Double.parseDouble(x0[i]),
                                                        Double.parseDouble(xmin[i]),
                                                        Double.parseDouble(xmax[i]),
                                                        Double.parseDouble(typx[i]));
                designVariables.add(x); 
                m_numDesignVariables++;
            }
            
            // Update control points
            cfd.updateControlPoints(designVariables);
        }
        catch (NumberFormatException e){
            // TO DO 
        }
    }
    
    // This method writes dependent variables from a CSV file  
    public void writeDependentVariables(String CSVfilepath, boolean isWriteGradient) throws IOException {
        
        // Extract data from CSV file
        CSVFile  csv  = new CSVFile(CSVfilepath); 
        try {
            
            // Get requested function names and types from file   
            m_numObjectiveFunctions = 0; 
            m_numInequalityConstraints = 0; 
            m_numEqualityConstraints = 0; 
            m_objectiveFunctionName = new ArrayList(); 
            m_inequalityConstraintName = new ArrayList(); 
            m_equalityConstraintName = new ArrayList(); 
            String[] type = csv.getColumn(m_functionTypeHeader);
            String[] func = csv.getColumn(m_functionNameHeader);
            for (int i = 0; i < type.length; i++) {
                switch (type[i]) {
                    case "Objective":
                        m_numObjectiveFunctions++; 
                        m_objectiveFunctionName.add(func[i]);
                        break;
                    case "Inequality":
                        m_numInequalityConstraints++;
                        m_inequalityConstraintName.add(func[i]);
                        break;
                    case "Equality":
                        m_numEqualityConstraints++;
                        m_equalityConstraintName.add(func[i]);
                        break;
                }
            }
            
            // Update list values for each function 
            updateObjectiveFunctionValues();
            updateInequalityConstraintValues();
            updateEqualityConstraintValues();
            if (isWriteGradient){
                updateObjectiveFunctionGradients();
                updateInequalityConstraintGradients();
                updateEqualityConstraintGradients();
            }
            
            // Convert list values to double arrays (for convenience)
            int nx = getNumberDesignVariables(); 
            int nf = getNumberObjectiveFunctions(); 
            int ng = getNumberInequalityConstraints(); 
            int nh = getNumberEqualityConstraints(); 
            double[] f = getObjectiveFunctions(); 
            double[] g = getInequalityConstraints(); 
            double[] h = getEqualityConstraints();
            double[][] dfdx = new double[nf][nx]; 
            double[][] dgdx = new double[ng][nx];
            double[][] dhdx = new double[nh][nx];
            if (isWriteGradient){
                for (int j = 0; j < nf; j++){System.arraycopy(getObjectiveGradient(j)           , 0, dfdx[j], 0, nx);}
                for (int j = 0; j < nh; j++){System.arraycopy(getEqualityConstraintGradient(j)  , 0, dhdx[j], 0, nx);}
                for (int j = 0; j < ng; j++){System.arraycopy(getInequalityConstraintGradient(j), 0, dgdx[j], 0, nx);}
            }
            
            // Finally, write values to file 
            // Format: FunctionType   FunctionName   Target   F   dFdX1   dFdX2...
            // 
            // Objective function(s): 
            int count = 0; 
            for (String functionName: m_objectiveFunctionName){
                int row = csv.findRowContainingItem(m_functionNameHeader,functionName);
                int col = csv.getColumnNumber(m_functionValueHeader); 
                csv.update(f[count],row,col); 
                if (isWriteGradient){
                    for (int j = 0; j < nx; j++){
                        String varname = designVariables.get(j).getVarName(); 
                        col = csv.getColumnNumber(m_partialPrefixHeader+varname);
                        csv.update(dfdx[count][j],row,col);
                    }
                }
                count++; 
            }
            
            // Equality constraint(s):
            count = 0; 
            for (String functionName: m_equalityConstraintName){
                int row = csv.findRowContainingItem(m_functionNameHeader,functionName);
                int col = csv.getColumnNumber(m_functionValueHeader); 
                csv.update(h[count],row,col); 
                if (isWriteGradient){
                    for (int j = 0; j < nx; j++){
                        String varname = designVariables.get(j).getVarName(); 
                        col = csv.getColumnNumber(m_partialPrefixHeader+varname);
                        csv.update(dhdx[count][j],row,col);
                    }
                }
                count++;
            }
            
            // Inequality constraint(s):
            count = 0; 
            for (String functionName: m_inequalityConstraintName){
                int row = csv.findRowContainingItem(m_functionNameHeader,functionName);
                int col = csv.getColumnNumber(m_functionValueHeader); 
                csv.update(g[count],row,col); 
                if (isWriteGradient){
                    for (int j = 0; j < nx; j++){
                        String varname = designVariables.get(j).getVarName(); 
                        col = csv.getColumnNumber(m_partialPrefixHeader+varname);
                        csv.update(dgdx[count][j],row,col);
                    }
                }
                count++;
            }
        }
        catch (NumberFormatException e){e.getMessage();}
    }
    
    // This method returns all design variable values as an array
    public double[] getDesignVariables(){
        double[] array = new double[m_numDesignVariables];
        for (int i = 0; i < m_numDesignVariables; i++){
            array[i] = designVariables.get(i).getCurrentValue(); 
        }
        return array; 
    }
    
    // This method returns typical values of the design variables (for scaling)
    public double[] getTypicalValues(){
        double[] array = new double[m_numDesignVariables];
        for (int i = 0; i < m_numDesignVariables; i++){
            array[i] = designVariables.get(i).getTypicalValue(); 
        }
        return array; 
    }
    
    // This method returns  all LB constraint values as an array
    public double[] getLowerBoundConstraints(){
        double[] array = new double[m_numDesignVariables];
        for (int i = 0; i < m_numDesignVariables; i++){
            array[i] = designVariables.get(i).getMinValue(); 
        }
        return array; 
    }
    
    // This method returns  all UB constraint values as an array
    public double[] getUpperBoundConstraints(){
        double[] array = new double[m_numDesignVariables];
        for (int i = 0; i < m_numDesignVariables; i++){
            array[i] = designVariables.get(i).getMaxValue(); 
        }
        return array; 
    }
    
    // This method returns  all objective function values as an array
    public double[] getObjectiveFunctions(){
        double[] array = new double[m_numObjectiveFunctions];
        for (int i = 0; i < m_numObjectiveFunctions; i++){
            array[i] = m_objectiveFunctionValue.get(i); 
        }
        return array; 
    }
    
    // This method returns  all inequality constraint values as an array
    public double[] getInequalityConstraints(){
        double[] array = new double[m_numInequalityConstraints];
        for (int i = 0; i < m_numInequalityConstraints; i++){
            array[i] = m_inequalityConstraintValue.get(i); 
        }
        return array; 
    }
    
    // This method returns  all equality constraint values as an array
    public double[] getEqualityConstraints(){
        double[] array = new double[m_numEqualityConstraints];
        for (int i = 0; i < m_numEqualityConstraints; i++){
            array[i] = m_equalityConstraintValue.get(i); 
        }
        return array; 
    }
    
    // This method returns  the nth objective function gradient values as an array
    public double[] getObjectiveGradient(int n){
        return m_objectiveFunctionGradient.get(n); 
    }
    
    // This method returns  the nth inequality constraint gradient values as an array
    public double[] getInequalityConstraintGradient(int n){
        return m_inequalityConstraintGradient.get(n); 
    }
    
    // This method returns  the nth equality constraint gradient values as an array
    public double[] getEqualityConstraintGradient(int n){
        return m_equalityConstraintGradient.get(n); 
    }
    
    // This methods updates the design variables 
    public void updateDesignVariables(double[] x){
        for (int i = 0; i < m_numDesignVariables; i++){
            designVariables.get(i).setCurrentValue(x[i]); 
        }
    }
    
    // This method returns the number of design variables 
    public int getNumberDesignVariables(){return m_numDesignVariables;} 
    
    // This method returns the number of objective functions 
    public int getNumberObjectiveFunctions(){return m_numObjectiveFunctions;}
 
    // This method returns the number of inequality constraints 
    public int getNumberInequalityConstraints(){return m_numInequalityConstraints;} 
    
    // This method returns the number of objective functions 
    public int getNumberEqualityConstraints(){return m_numEqualityConstraints;}
 
    // This method returns the name of the objective function 
    public String getObjectiveFunctionName(int index){
        return m_objectiveFunctionName.get(index);
    } 

    // This method returns the name of the inequality constraint
    public String getInequalityConstraintName(int index){
        return m_inequalityConstraintName.get(index);
    }
    
    // This method returns the name of the equality constraints 
    public String getEqualityConstraintName(int index){
        return m_equalityConstraintName.get(index);
    }
    
    // This method sets (gets) the input table name
    public void setFunctionTypeHeader(String s){m_functionTypeHeader = s;}
    public String getFunctionTypeHeader(){return m_functionTypeHeader;}
    
    // This method sets (gets) the input table name
    public void setReportNameHeader(String s){m_functionNameHeader = s;}
    public String getReportNameHeader(){return m_functionNameHeader;}
    
    // This method sets (gets) the input table name
    public void setFunctionValueHeader(String s){m_functionValueHeader = s;}
    public String getFunctionValueHeader(){return m_functionValueHeader;}
    
    // This method sets (gets) the input table name
    public void setPartialPrefixHeader(String s){m_partialPrefixHeader = s;}
    public String getPartialPrefixHeader(){return m_partialPrefixHeader;}
    
    // This method sets (gets) the "Xval" column name expected in the input table
    public void setXvalHeader(String s){m_xvalHeader = s;}
    public String getXvalHeader(){return m_xvalHeader;}

    // This method sets (gets) the "Xval" column name expected in the input table
    public void setVarNameHeader(String s){m_varnameHeader = s;}
    public String getVarNameHeader(){return m_varnameHeader;}

    // This method sets (gets) the "Xmin" column name expected in the input table
    public void setXminHeader(String s){m_xminHeader = s;}
    public String getXminHeader(){return m_xminHeader;}

    // This method sets (gets) the "Xmax" column name expected in the input table
    public void setXmaxHeader(String s){m_xmaxHeader = s;}
    public String getXmaxHeader(){return m_xmaxHeader;}

    // This method sets (gets) the "TypicalX" column name expected in the input table
    public void setTypicalXHeader(String s){m_typicalXHeader = s;}
    public String getTypicalXHeader(){return m_typicalXHeader;}

    // This method sets (gets) the "ControlPointTableID" column name expected in the input table
    public void setControlPointTableIDHeader(String s){m_controlPointTableIDHeader = s;}
    public String getControlPointTableIDHeader(){return m_controlPointTableIDHeader;}

    // This method sets (gets) the "ControlPointTableRow" column name expected in the input table
    public void setControlPointTableRowHeader(String s){m_controlPointTableRowHeader = s;}
    public String getControlPointTableRowHeader(){return m_controlPointTableRowHeader;}

    // This method sets (gets) the "ControlPointTableCol" column name expected in the input table
    public void getControlPointTableColHeader(String s){m_controlPointTableColHeader = s;}
    public String getControlPointTableColHeader(){return m_controlPointTableColHeader;}
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // This method retrieves the objective functions values
    private void updateObjectiveFunctionValues(){
        if (m_numObjectiveFunctions > 0) {
            getFunctionValues(m_objectiveFunctionValue,m_objectiveFunctionName);
        }
    }
    
    // This method retrieves the inequality constraint values
    private void updateInequalityConstraintValues(){
        if (m_numInequalityConstraints > 0) {
            getFunctionValues(m_inequalityConstraintValue,m_inequalityConstraintName);
        }
    }
    
    // This method retrieves the equality constraint values
    private void updateEqualityConstraintValues(){
        if (m_numEqualityConstraints > 0) {
            getFunctionValues(m_equalityConstraintValue,m_equalityConstraintName);
        }
    }
    
    // This method retrieves the values corresponding to the function in the list
    private void getFunctionValues(ArrayList<Double> values, ArrayList<String> functionNames){
        int numberItems = functionNames.size();
        if (numberItems > 0){
            values.clear(); 
            for (String functionName: functionNames){
                Double value = cfd.getFunctionValue(functionName); 
                values.add(value);  
            }
        }
    }
    
    // This method updates the objective functions gradient values
    private void updateObjectiveFunctionGradients(){
        if (m_numObjectiveFunctions > 0) {
            getGradientValues(m_objectiveFunctionGradient, m_objectiveFunctionName);
        }
    }
    
    // This method updates the objective functions gradient values
    private void updateInequalityConstraintGradients(){
        if (m_numInequalityConstraints > 0) {
            getGradientValues(m_inequalityConstraintGradient, m_inequalityConstraintName);
        }
    }
    
    // This method updates the objective functions gradient values
    private void updateEqualityConstraintGradients(){
        if (m_numEqualityConstraints > 0) {
            getGradientValues(m_equalityConstraintGradient, m_equalityConstraintName);
        }
    }
    
    // This method updates the objective functions gradient values
    private void getGradientValues(ArrayList<double[]> values, ArrayList<String> functionNames){
        int numberItems = functionNames.size(); 
        if (numberItems > 0){
            values.clear(); 
            for (String functionName: functionNames){
                double[] grad = new double[m_numDesignVariables]; 
                for (int i = 0; i < m_numDesignVariables; i++){
                    grad[i] = cfd.getPartialDerivative(functionName,designVariables.get(i)); 
                } 
                values.add(grad);  
            }
        }
    }
}
