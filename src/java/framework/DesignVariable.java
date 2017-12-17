/*
 * This class creates a "design variable" object. It is intented to work with 
 * Starccm+ where a design variable is assumed to be the X, Y, or Z coordinate 
 * of a "control point." Moving a control point deforms the surface mesh.
 */

package framework;

/**
 *
 * @author ASDL
 */
public class DesignVariable {
    
    // --- Properties 
    private 
        static int m_count; // counts number of DesignVariable objects created
        int m_id; // unique ID (allocated to each object at instantiation)  	
        String m_controlPointTableID; // table ID as "ControlPointTable_0" (ID = 0)
        String m_variableName; 
        int m_controlPointTableRow; // control point table row location
        int m_controlPointTableCol; // control point table col location
        double m_initialValue;  // initial value at time of creation
        double m_currentValue;	// current value of design variable
        double m_minimumValue;	// minimum allowed value of design variable
        double m_maximumValue;	// maximum allowed value of design variable
        double m_typicalValue;  // typical value of design variable(for scaling)
   
    // --- Constructors 
    public DesignVariable(  String controlPointTableID, 
                            String variableName,
                            int controlPointTableRow,
                            int controlPointTableCol, 
                            double currentValue,
                            double initialValue,
                            double minimumValue,
                            double maximumValue,
                            double typicalValue){
        m_id = m_count++; 
        m_variableName = variableName; 
        m_controlPointTableID = controlPointTableID;
        m_controlPointTableRow = controlPointTableRow;
        m_controlPointTableCol = controlPointTableCol;  
        m_currentValue = currentValue; 
        m_initialValue = initialValue;
        m_minimumValue = minimumValue; 
        m_maximumValue = maximumValue; 
        m_typicalValue = typicalValue; 
    }   
    
    // --- Methods
    public int      getID(){return m_id;}
    public String   getVarName(){return m_variableName;}
    public void     setCurrentValue(double x){m_currentValue = x;}
    public double   getCurrentValue(){return m_currentValue;}
    public double   getInitialValue(){return m_initialValue;}
    public double   getTypicalValue(){return m_typicalValue;}
    public void     setMinValue(double x){m_minimumValue = x;}
    public double   getMinValue(){return m_minimumValue;}
    public void     setMaxValue(double x){m_maximumValue = x;}
    public double   getMaxValue(){return m_maximumValue;}
    public String   getControlPointTableID(){return m_controlPointTableID;}
    public int      getControlPointTableRow(){return m_controlPointTableRow;}
    public int      getControlPointTableCol(){return m_controlPointTableCol;}
    
}
