/*
TITLE: CST Airfoil Representation Class
--------------------------------------------------------------------------------
WRITTEN BY: Jai Ahuja
--------------------------------------------------------------------------------
DESCRIPTION: This class provides the following outputs:
    1) Airfoil max thickness  
    2) Gradient of the max thickness w.r.t y displacements of the control points
This class also allows the user to define the number of CST coefficients desired
for the fit - must be less than the number of control points for a given surface
--------------------------------------------------------------------------------
LAST MODIFIED: Jai Ahuja (06/03/2017 1624)
--------------------------------------------------------------------------------
DEVELOPMENT NOTES: 
    1) The number of control points defining the airfoil must be greater than 
    the number of CST coefficients required for the least squares regression to 
    work well
    2) Coordinates are assumed to be normalized by the chord length, and 
    represent an untwisted airfoil
`   3) Same number of CP required for both upper and lower surfaces. Also 
    assuming CP are defined for the same x values. Assumed format (LE-TE)
    x       yu          yl
    0.0     ###         ###
    ...     ...         ...
    1.0     ###         ###
    4) Gradient definitions follow the same definitions as CP - LE to TE on both
    upper and lower
--------------------------------------------------------------------------------
*/

package framework;

import static java.lang.Math.pow; 
import static java.lang.Math.sqrt;
import static org.apache.commons.math3.linear.MatrixUtils.createRealMatrix;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;
import org.apache.commons.math3.linear.RealMatrix;
import static org.apache.commons.math3.util.ArithmeticUtils.factorial;

public class CST {
    
    //--------------------------------------------------------------------------
    //1.0 PROPERTIES
    //--------------------------------------------------------------------------
    
    
    //1.1 User Defined Inputs    
    protected int m_n = 8;                //Number of CST coefficients desired (default value can be changed)
    protected double[][] m_x;             //Control point x coordinates
    protected double[][] m_yu;            //Upper surface y coordinates
    protected double[][] m_yl;            //Lower surface y coordinates 
    //1.2 Derived from User Inputs
    protected double m_yteu;              //Upper surface trailing edge thickness
    protected double m_ytel;              //Lower surface trailing edge thickness
    protected int m_numCP;                //Total number of control points defined
    //1.3 Key Components Calculated on Initialization
    protected double[] m_K;               //Binomial coefficients
    protected RealMatrix m_pseudoInv;     //Pseudoinverse for least squares determination of CST coeffs
    protected RealMatrix m_Au;            //Upper surface CST coefficients
    protected RealMatrix m_Al;            //Lower surface CST coefficients
    //1.4 Values Required by Methods 
    protected double m_xMaxThick;         //X location of max thickness

    //--------------------------------------------------------------------------
    //2.0 CONSTRUCTOR
    //--------------------------------------------------------------------------
        
    public CST(double[][] x, double[][] yu, double[][] yl){ 
        m_x = x;
        m_yu = yu;
        m_yl = yl;
        m_numCP = 2*x.length;
        m_yteu = m_yu[m_yu.length-1][0];     
        m_ytel = m_yl[m_yl.length-1][0];
        m_K = getBinomialCoeffs();
        m_pseudoInv =  getPseudoInverse();      
        m_Au = getCstCoeffs(m_yu,m_yteu);  
        m_Al = getCstCoeffs(m_yl,m_ytel); 
    } 

    CST() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    //--------------------------------------------------------------------------
    //3.0 MAIN METHODS - PUBLIC
    //--------------------------------------------------------------------------
    
    //3.1 Airfoil Max Thickness Function
    public double getAirfoilMaxThickness() {
        double maxThickness = 0; double maxThicknesGuess = 0; 
        double xtest = 0; 
        do {
           maxThickness = maxThicknesGuess;
           xtest = xtest+0.01;      //Marching along airfoil chord  in increments of 0.01 from 0 to xmax
           maxThicknesGuess = getAirfoilThickness(xtest);
        } while (maxThicknesGuess>maxThickness);
        m_xMaxThick = xtest-0.01;
        return(maxThickness);
    }
    
    //3.2 Gradient of Max Thickness w.r.t Control Point y Displacements
    public double[][] getMaxThickConstGrad (){
        double[][] S = new double[m_numCP][1];    
        RealMatrix SFG = createRealMatrix(S);
        RealMatrix dAudy = createRealMatrix(S);      //Derivative of the upper surface coefficients w.r.t. y
        RealMatrix dAldy = createRealMatrix(S);      //Derivative of the lower surface coefficients w.r.t. y
        double [][] gradMagnitude = new double[m_numCP/2][1];  //Gradients for each coefficient have the same magnitude on the upper and lower surfaces
        for (int i = 0; i<m_n; i++){
            gradMagnitude = m_pseudoInv.getRowMatrix(i).transpose().getData();
            dAudy.setSubMatrix(gradMagnitude,0,0);
            dAldy.setSubMatrix(gradMagnitude,m_numCP/2,0);
            SFG = SFG.add((dAudy.subtract(dAldy)).scalarMultiply(m_K[i]*pow(m_xMaxThick,i)*pow(1-m_xMaxThick,m_n-i-1)));
        } 
        double[][] maxThickConstGrad = SFG.scalarMultiply(sqrt(m_xMaxThick)*(1-m_xMaxThick)).getData();
        return(maxThickConstGrad);    
        }
    
    //3.3 Setting the Number of CST Coeffs to a User Defined Value
    public void setNumberofCstCoeffs(int number) {m_n = number;}

    //--------------------------------------------------------------------------
    //4.0 SUPPORTING METHODS - PRIVATE
    //--------------------------------------------------------------------------
    
    //4.1 Calculating the PseudoInverse for Least Squares Fitting of CST Coeffs
    private RealMatrix getPseudoInverse() {
        double[][] phi = new double[m_numCP/2][m_n]; 
        for (int i = 0; i<m_numCP/2; i++){
           for(int j = 0; j<m_n; j++){
               phi[i][j] = sqrt(m_x[i][0])*(1-m_x[i][0])*m_K[j]*(pow(m_x[i][0],j))*(pow((1-m_x[i][0]),(m_n-1-j)));
            }
        }
        RealMatrix PHI = createRealMatrix(phi); 
        RealMatrix pseudoInvPhi = inverse(PHI.transpose().multiply(PHI)).multiply(PHI.transpose()); 
        return(pseudoInvPhi);
    }
    
    //4.2 Generating the CST Coeffs from the PseudoInverse
    private RealMatrix getCstCoeffs(double[][] y, double yte) {
        RealMatrix Y = createRealMatrix(y);    
        RealMatrix X = createRealMatrix(m_x);   
        RealMatrix A = m_pseudoInv.multiply(Y.subtract(X.scalarMultiply(yte))); //CST coefficients
        return(A);
        }

    //4.3 General Function to Calculate Airfoil Thickness at any x Location
    private double getAirfoilThickness(double xtest) {
        double SF = 0;  
        for (int i = 0; i<m_n; i++){
            SF = SF+((m_Au.getEntry(i, 0)-m_Al.getEntry(i, 0))*m_K[i]*pow(xtest,i)*pow(1-xtest,m_n-i-1));
        }
        double thickness = (sqrt(xtest)*(1-xtest)*SF)+xtest*(m_yteu-m_ytel);
        return(thickness);
        }
    
    //4.4 Function to Calculate Binomial Coefficients for CST
    private double[] getBinomialCoeffs(){
        double[] K = new double[m_n];
        for (int i = 0; i<m_n; i++){
            K[i] = factorial(m_n-1)/(factorial(i)*factorial(m_n-1-i));
        }
        return(K);
    }
}