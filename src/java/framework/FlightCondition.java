/*
 * This class contains information about the flight conditions, assuming 
 * external flow. It creates global parameters for alpha, beta, M, P, T, rho, 
 * a, mu which can be linked to reports and boundary conditions. Such a setup is 
 * convenient because values only need to be changed in one place. 
 * 
 * This class also provides public methods that can be used to reconfigure 
 * an existing simulation so to take advantage of global parameters. Specifical-
 * ly, assuming freesream boundary conditions on the farfield, these methods 
 * will link reports, boundary and initial conditions to the global parameters. 
 */

package framework;

import java.util.Collection;
import star.base.report.Report;
import star.common.*;
import star.energy.*;
import star.flow.*;
import star.material.*;

/**
 *
 * @author shb
 */
public class FlightCondition {
    
    // -------------------------------------------------------------------------
    // ---------------------- A T T R I B U T E S ------------------------------
    // -------------------------------------------------------------------------
    
    private final Simulation m_simulation; 
    private final Toolbox m_toolbox; 
    private boolean m_isFlow2D; 
    private boolean m_isInviscid; 
    private double m_dynamicViscosity; 
    private double m_machNumber; 
    private double m_angleOfAttack; 
    private double m_referencePressure; 
    private double m_freestreamGaugePressure; // P = Pref + Pgauge
    private double m_freestreamTemperature; 
    private double m_freestreamDensity; 
    private double m_freestreamSpeedOfSound;
    private String m_dragCoefficientReportName;
    private String m_liftCoefficientReportName;
    
    // -------------------------------------------------------------------------
    // ---------------------- C O N S T R U C T O R ----------------------------
    // -------------------------------------------------------------------------
    
    public FlightCondition(Simulation sim) {
        // NASA CRM flight conditions (M = 0.85, Re = 40M, alpha = 2)
        // ---------------------------------------------------------------------
        // Altitude	Temp        Pressure	Density     Viscosity       SoS
        // m            K           10^4 N/m2	kg/m3       10-5 N.s/m2     m/s
        // 11753	216.6785    2.0099	0.3232      1.4185          295.07
        // ---------------------------------------------------------------------
        m_simulation = sim;
        m_toolbox = new Toolbox(sim); 
        m_isFlow2D = false; 
        m_isInviscid = false; 
        m_angleOfAttack = 2; // deg
        m_machNumber = 0.85; 
        m_freestreamTemperature = 216.6785; // K
        m_freestreamDensity = 0.3232; // kg/m3
        m_freestreamSpeedOfSound = 295.07; // m/s
        m_referencePressure =  20099; // Pa
        m_freestreamGaugePressure = 0.0; // Pa 
        m_dynamicViscosity = 0.000014185; // Pa-s
        m_dragCoefficientReportName = "CD";
        m_liftCoefficientReportName = "CL";
        createGlobalParameters();
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    public void setEulerFlag(boolean b){
        m_isInviscid = b;
    }
    public void set2DFlag(boolean b){
        m_isFlow2D = b;
    }
    public void setMachNumber(double d){
        m_machNumber = d;
        updateGlobalParameters();
    }
    public void setAngleOfAttack(double d){
        m_angleOfAttack = d;
        updateGlobalParameters();
    }
    public void setReferencePressure(double d){
        m_referencePressure = d;
        updateGlobalParameters();
    }
    public void setFreestreamGaugePressure(double d){
        m_freestreamGaugePressure = d;
        updateGlobalParameters();
    }
    public void setFreestreamTemperature(double d){
        m_freestreamTemperature = d;
        updateGlobalParameters();
    }
    public void setFreestreamDensity(double d){
        m_freestreamDensity = d;
        updateGlobalParameters();
    }
    public void setFreestreamSpeedOfSound(double d){
        m_freestreamSpeedOfSound = d;
        updateGlobalParameters();
    }
    public void setFreestreamDynamicViscosity(double d){
        m_dynamicViscosity = d;
        updateGlobalParameters();
    }
    public void setDragCoefficientReportName(String s){m_dragCoefficientReportName= s;}
    public void setLiftCoefficientReportName(String s){m_liftCoefficientReportName= s;}
    
    public double getMachNumber(){return m_machNumber;}
    public double getAngleOfAttack(){return m_angleOfAttack;}
    public double getReferencePressure(){return m_referencePressure;}
    public double getFreestreamGaugePressure(){return m_freestreamGaugePressure;}
    public double getFreestreamTemperature(){return m_freestreamTemperature;}
    public double getFreestreamDensity(){return m_freestreamDensity;}
    public double getFreestreamSpeedOfSound(){return m_freestreamSpeedOfSound;}
    public double getFreestreamDynamicViscosity(){return m_dynamicViscosity;}
    public boolean getEulerFlag(){return m_isInviscid;}
    public boolean get2DFlag(){return m_isFlow2D;}
    public String getDragCoefficientReportName(){return m_dragCoefficientReportName;}
    public String getLiftCoefficientReportName(){return m_liftCoefficientReportName;}
    
    // This method links the global parameters to: 
    // (1) Freestream boundary conditions 
    // (2) Initial conditions 
    // (3) Gas properties
    // (4) Force reports 
    // (5) Force coefficient reports
    // (6) Moment coefficient reports 
    public void linkGlobalParameters(String fluidRegionName,
                                     String freestreamBoundaryName, 
                                     String physicsContinuumName){
        linkFreestreamBC(fluidRegionName,freestreamBoundaryName);
        linkFreestreamIC(physicsContinuumName);
        linkPhysicsProperties(physicsContinuumName);
        linkForceReports();
        linkForceCoefficientReports();
        linkMomentCoefficientReports();
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // This method links the freestream BC to global parameters via formulas
    private void linkFreestreamBC(String fluidRegionName,String freestreamBoundaryName){

        Region fluid = m_simulation.getRegionManager().getRegion(fluidRegionName); 
        Boundary farfield = fluid.getBoundaryManager().getBoundary(freestreamBoundaryName); 

        // Mach Number
        MachNumberProfile machNumberProfile_0 = farfield.getValues().get(MachNumberProfile.class);
        machNumberProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${M}");

        // Temperature
        StaticTemperatureProfile temperatureProfile_0 = farfield.getValues().get(StaticTemperatureProfile.class);
        temperatureProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${T}");

        // Pressure 
        StaticPressureProfile staticPressureProfile_0 = farfield.getValues().get(StaticPressureProfile.class);
        staticPressureProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${P}");

        // Angle of attack (flow direction)
        farfield.getConditions().get(FlowDirectionOption.class).setSelected(FlowDirectionOption.Type.COMPONENTS);
        FlowDirectionProfile flowDirectionProfile = farfield.getValues().get(FlowDirectionProfile.class);
        if (m_isFlow2D){
            // X
            ScalarProfile scalarProfile_0 = flowDirectionProfile.getMethod(CompositeVectorProfileMethod.class).getProfile(0);
            scalarProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("cos($alpha)");
            // Y
            ScalarProfile scalarProfile_1 = flowDirectionProfile.getMethod(CompositeVectorProfileMethod.class).getProfile(1);
            scalarProfile_1.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("sin($alpha)");
            // Z
            ScalarProfile scalarProfile_2 = flowDirectionProfile.getMethod(CompositeVectorProfileMethod.class).getProfile(2);
            scalarProfile_2.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("0.0");
        }
        else {
            // X
            ScalarProfile scalarProfile_0 = flowDirectionProfile.getMethod(CompositeVectorProfileMethod.class).getProfile(0);
            scalarProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("cos($alpha)");
            // Y
            ScalarProfile scalarProfile_1 = flowDirectionProfile.getMethod(CompositeVectorProfileMethod.class).getProfile(1);
            scalarProfile_1.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("0");
            // Z
            ScalarProfile scalarProfile_2 = flowDirectionProfile.getMethod(CompositeVectorProfileMethod.class).getProfile(2);
            scalarProfile_2.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("sin($alpha)");
        }
    }
    
    // This method links the freestream BC to global parameters via formulas
    private void linkFreestreamIC(String physicsContinuumName){
        
        PhysicsContinuum physicsContinuum_0 = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
        
        InitialPressureProfile initialPressureProfile_0 = physicsContinuum_0.getInitialConditions().get(InitialPressureProfile.class);
        initialPressureProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${P}");

        StaticTemperatureProfile staticTemperatureProfile_0 = physicsContinuum_0.getInitialConditions().get(StaticTemperatureProfile.class);
        staticTemperatureProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${T}");

        VelocityProfile velocityProfile_0 = physicsContinuum_0.getInitialConditions().get(VelocityProfile.class);
        velocityProfile_0.setMethod(CompositeVectorProfileMethod.class);
        if (m_isFlow2D){
            // X
            ScalarProfile scalarProfile_0 = velocityProfile_0.getMethod(CompositeVectorProfileMethod.class).getProfile(0);
            scalarProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("cos(${alpha})*${M}*${a}");
            // Y
            ScalarProfile scalarProfile_1 = velocityProfile_0.getMethod(CompositeVectorProfileMethod.class).getProfile(1);
            scalarProfile_1.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("sin(${alpha})*${M}*${a}");
            // Z
            ScalarProfile scalarProfile_2 = velocityProfile_0.getMethod(CompositeVectorProfileMethod.class).getProfile(2);
            scalarProfile_2.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("0.0");
        }
        else {
            // X
            ScalarProfile scalarProfile_0 = velocityProfile_0.getMethod(CompositeVectorProfileMethod.class).getProfile(0);
            scalarProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("cos(${alpha})*${M}*${a}");
            // Y
            ScalarProfile scalarProfile_2 = velocityProfile_0.getMethod(CompositeVectorProfileMethod.class).getProfile(1);
            scalarProfile_2.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("0.0");
            // Z
            ScalarProfile scalarProfile_1 = velocityProfile_0.getMethod(CompositeVectorProfileMethod.class).getProfile(2);
            scalarProfile_1.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("sin(${alpha})*${M}*${a}");
        }
    }
    
    // This method links the physics continuum properties to global parameters
    private void linkPhysicsProperties(String physicsContinuumName){
        
        PhysicsContinuum physicsContinuum_0 = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
        physicsContinuum_0.getReferenceValues().get(ReferencePressure.class).setDefinition("${Pref}");
        
        if (!m_isInviscid){
            SingleComponentGasModel singleComponentGasModel_0 = physicsContinuum_0.getModelManager().getModel(SingleComponentGasModel.class);
            Gas gas_0 = ((Gas) singleComponentGasModel_0.getMaterial());
            ConstantMaterialPropertyMethod constantMaterialPropertyMethod_0 = ((ConstantMaterialPropertyMethod) gas_0.getMaterialProperties().getMaterialProperty(DynamicViscosityProperty.class).getMethod());
            constantMaterialPropertyMethod_0.getQuantity().setDefinition("${mu}");
        }
    }
    
    // This method links force coefficient reports to the global parameters
    private void linkForceCoefficientReports(){
        Collection<Report> reports = m_simulation.getReportManager().getObjects(); 
        for (Report report: reports){
            String name = report.getPresentationName(); 
            if (report.getClass().equals(ForceCoefficientReport.class)){
                ForceCoefficientReport forceCoefficientReport = ((ForceCoefficientReport) m_simulation.getReportManager().getReport(name));
                forceCoefficientReport.getReferenceVelocity().setDefinition("${a}*${M}");
                forceCoefficientReport.getReferenceDensity().setDefinition("${rho}");
                if (m_isFlow2D){
                    if (name.equals(m_liftCoefficientReportName)) {
                        forceCoefficientReport.getDirection().setDefinition("[-sin(${alpha}), cos(${alpha}), 0]");
                    }
                    if (name.equals(m_dragCoefficientReportName)) {
                        forceCoefficientReport.getDirection().setDefinition("[cos(${alpha}), sin(${alpha}), 0]");
                    }
                }
                else {
                    if (name.equals(m_liftCoefficientReportName)) {
                        forceCoefficientReport.getDirection().setDefinition("[-sin(${alpha}), 0, cos(${alpha})]");
                    }
                    if (name.equals(m_dragCoefficientReportName)) {
                        forceCoefficientReport.getDirection().setDefinition("[cos(${alpha}), 0, sin(${alpha})]");
                    }
                }
            }
        }
    }
    
    // This method links force coefficient reports to the global parameters
    private void linkMomentCoefficientReports(){
        Collection<Report> reports = m_simulation.getReportManager().getObjects(); 
        for (Report report: reports){
            String name = report.getPresentationName(); 
            if (report.getClass().equals(MomentCoefficientReport.class)){
                MomentCoefficientReport momentCoefficientReport = ((MomentCoefficientReport) m_simulation.getReportManager().getReport(name));
                momentCoefficientReport.getReferenceVelocity().setDefinition("${a}*${M}");
                momentCoefficientReport.getReferenceDensity().setDefinition("${rho}");
            }
        }
    }
    
    // This method links force reports to the global parameters
    private void linkForceReports(){
        Collection<Report> reports = m_simulation.getReportManager().getObjects(); 
        for (Report report: reports){
            String name = report.getPresentationName(); 
            if (report.getClass().equals(ForceReport.class)){
                ForceReport forceReport = ((ForceReport) m_simulation.getReportManager().getReport(name));
                if (m_isFlow2D){
                    forceReport.getDirection().setDefinition("[-sin(${alpha}), cos(${alpha}), 0]");
                }
                else {
                    forceReport.getDirection().setDefinition("[-sin(${alpha}), 0, cos(${alpha})]");
                }
            }
        }
    }
    
    // This method creates global parameters (if they don't already exist) 
    private void createGlobalParameters(){
        m_toolbox.createGlobalScalarParameter("M",m_machNumber);
        m_toolbox.createGlobalScalarParameter("alpha",m_angleOfAttack,"deg");
        m_toolbox.createGlobalScalarParameter("Pref",m_referencePressure,"Pa");
        m_toolbox.createGlobalScalarParameter("P",m_freestreamGaugePressure,"Pa");
        m_toolbox.createGlobalScalarParameter("T",m_freestreamTemperature,"K");
        m_toolbox.createGlobalScalarParameter("rho",m_freestreamDensity,"kg/m3");
        m_toolbox.createGlobalScalarParameter("a",m_freestreamSpeedOfSound,"m/s");
        m_toolbox.createGlobalScalarParameter("mu",m_dynamicViscosity,"N.s/m2");
    }
    
    // This method updates the global parameter values
    private void updateGlobalParameters(){
        m_toolbox.updateGlobalScalarParameter("M",m_machNumber);
        m_toolbox.updateGlobalScalarParameter("alpha",m_angleOfAttack);
        m_toolbox.updateGlobalScalarParameter("Pref",m_referencePressure);
        m_toolbox.updateGlobalScalarParameter("P",m_freestreamGaugePressure);
        m_toolbox.updateGlobalScalarParameter("T",m_freestreamTemperature);
        m_toolbox.updateGlobalScalarParameter("rho",m_freestreamDensity);
        m_toolbox.updateGlobalScalarParameter("a",m_freestreamSpeedOfSound);
        m_toolbox.updateGlobalScalarParameter("mu",m_dynamicViscosity);
    }
}
