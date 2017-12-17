/*
 * The purpose of this toolbox is to provide a generic set of stand-alone 
 * methods shared across subclasses. Usually things like create, rename, delete, 
 * etc. The toolbox is here to keep subclasses as uncluttered as possible. 
 */

package framework;

import java.util.ArrayList;
import java.util.Collection;
import star.base.neo.DoubleVector;
import star.base.neo.NeoObjectVector;
import star.base.report.Monitor;
import star.base.report.MonitorManager;
import star.base.report.Report;
import star.base.report.ReportManager;
import star.base.report.ReportMonitor;
import star.base.report.SumReport;
import star.base.report.VolumeAverageReport;
import star.base.report.ElementCountReport;
import star.cadmodeler.CadModel;
import star.cadmodeler.SolidModelManager;
import star.common.AdjointCostFunction;
import star.common.AdjointCostFunctionManager;
import star.common.Boundary;
import star.common.CadModelBase;
import star.common.ConstantScalarProfileMethod;
import star.common.ConstantVectorProfileMethod;
import star.common.Continuum;
import star.common.ContinuumManager;
import star.common.FeatureCurve;
import star.common.FieldFunctionManager;
import star.common.FieldFunctionTypeOption;
import star.common.FreeStreamBoundary;
import star.common.GeometryPart;
import star.common.ManagerManager;
import star.common.ModelManager;
import star.common.MonitorPlot;
import star.common.NullFieldFunction;
import star.common.Part;
import star.common.PartCurve;
import star.common.PartManager;
import star.common.PartSurface;
import star.common.PhysicsContinuum;
import star.common.PlotManager;
//import star.common.PointSet; 
//import star.common.PointSetManager; 
import star.common.PrimitiveFieldFunction;
import star.common.Region;
import star.common.RegionManager;
import star.common.Simulation;
import star.common.SimulationPartManager;
import star.common.Solution;
import star.common.StarPlot;
import star.common.SymmetryBoundary;
import star.common.Table;
import star.common.TableData;
import star.common.TableManager;
import star.common.Units;
import star.common.UserFieldFunction;
import star.common.VectorComponentFieldFunction;
import star.common.WallBoundary;
import star.common.XyzInternalTable;
import star.coupledflow.AdjointFlowModel;
import star.coupledflow.ForceCostFunction;
import star.coupledflow.MomentCostFunction;
import star.energy.StaticTemperatureProfile;
import star.flow.AltitudeProfile;
import star.flow.DynamicViscosityProperty;
import star.flow.FlowDirectionProfile;
import star.flow.ForceCoefficientReport;
import star.flow.ForceReport;
import star.flow.ForceReportForceOption;
import star.flow.FreeStreamOption;
import star.flow.InitialPressureProfile;
import star.flow.MachNumberProfile;
import star.flow.MomentCoefficientReport;
import star.flow.MomentReport;
import star.flow.ReferencePressure;
import star.flow.StaticPressureProfile;
import star.flow.VelocityProfile;
import star.material.ConstantMaterialPropertyMethod;
import star.material.DataBaseGas;
import star.material.DataBaseMaterialConstantPropertyMethod;
import star.material.DataBaseMaterialManager;
import star.material.DataBaseMaterialPropertyDensity;
import star.material.DataBaseMaterialPropertyMu;
import star.material.Gas;
import star.material.MaterialDataBase;
import star.material.MaterialDataBaseManager;
import star.material.SingleComponentGasModel;
import star.meshing.AutoMeshOperation2d;
import star.meshing.CadPart;
import star.meshing.GenericAbsoluteSize;
import star.meshing.GenericRelativeSize;
import star.meshing.MeshActionManager;
import star.meshing.MeshOperation;
import star.meshing.MeshOperationManager;
import star.meshing.MeshPart;
import star.meshing.MeshPipelineController;
import star.meshing.PartsMinimumSurfaceSize;
import star.meshing.PartsMinimumSurfaceSizeOption;
import star.meshing.PartsTargetSurfaceSize;
import star.meshing.PartsTargetSurfaceSizeOption;
import star.meshing.PrepareFor2dOperation;
import star.meshing.RelativeOrAbsoluteOption;
import star.meshing.SurfaceCustomMeshControl;
//import star.morpher.ControlPointRegion; // starccm+ version 11.04.12
//import star.morpher.ControlPointRegionManager; // starccm+ version 11.04.12
import star.prismmesher.CustomPrismValuesManager;
import star.prismmesher.NumPrismLayers;
import star.prismmesher.PartsCustomPrismsOption;
import star.prismmesher.PartsCustomizePrismMesh;
import star.prismmesher.PartsCustomizePrismMeshControls;
import star.prismmesher.PrismThickness;
import star.prismmesher.PrismWallThickness;
import star.vis.Scene;
import star.vis.SceneManager;
import star.vis.ThresholdPart;
import star.common.*;
import star.base.neo.*;
import star.base.report.ExpressionReport;
import star.coupledflow.BoundaryParameterOption;
import star.coupledflow.BoundarySensitivitiesReport;
import star.prismmesher.PrismAutoMesher;
import star.prismmesher.PrismStretchingOption;

/**
 *
 * @author shb
 */
public class Toolbox {
    
    // --- Properties  
    private Simulation m_simulation;
    
    // --- Constructor 
    public Toolbox(Simulation sim) {
        m_simulation = sim; 
    }

    // --- Methods 
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------  
    
    // ---------------------- activate -----------------------------------------
    
     // This method activates the adjoint solver
     public void activateAdjointSolver(String physicsContinuumName){
        PhysicsContinuum physicsContinuum = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
        ModelManager modelManager = physicsContinuum.getModelManager(); 
        if (modelManager.has("Adjoint Flow")){
            AdjointFlowModel adjointFlow = physicsContinuum.getModelManager().getModel(AdjointFlowModel.class);
            physicsContinuum.disableModel(adjointFlow);
        }
        physicsContinuum.enable(AdjointFlowModel.class);
     }
     
     // ---------------------- clear -----------------------------------------
     
     public void clearSolution() {
         Solution solution = m_simulation.getSolution();
         solution.clearSolution(Solution.Clear.History, Solution.Clear.Fields, Solution.Clear.AdjointFlow);
  }
    
    // ---------------------- creation -----------------------------------------
    
    // This method creates a badge for 2D meshing 
    public void createBadgeFor2DMeshing(String partName){
       String badgeName = partName + " 2D Badge";
       MeshPart meshPart = ((MeshPart) m_simulation.get(SimulationPartManager.class).getPart(partName));
       PrepareFor2dOperation prepareFor2dOperation = (PrepareFor2dOperation) m_simulation.get(MeshOperationManager.class).createPrepareFor2dOperation(new NeoObjectVector(new Object[] {meshPart}));
       prepareFor2dOperation.setPresentationName(badgeName);
       prepareFor2dOperation.execute();
    }
    
     // This method creates a boundary parameter sensitivity report.
     public void createBoundaryParameterSensitivityReport(String regionName, 
                                                             String boundaryName,
                                                             String adjointCostFunctionName,
                                                             String parameterName,
                                                             String reportName) {
        AdjointCostFunctionManager adjointManager = m_simulation.getAdjointCostFunctionManager(); 
        ReportManager reportManager = m_simulation.getReportManager();
        if (adjointManager.has(adjointCostFunctionName) & !reportManager.has(reportName)){
           BoundarySensitivitiesReport boundarySensitivitiesReport = reportManager.createReport(BoundarySensitivitiesReport.class);
           ForceCostFunction forceCostFunction = ((ForceCostFunction) m_simulation.get(AdjointCostFunctionManager.class).getAdjointCostFunction(adjointCostFunctionName));
           boundarySensitivitiesReport.setAdjointCostFunction(forceCostFunction);
           switch (parameterName){
                   case "FLOW_DIRECTION_X":
                       boundarySensitivitiesReport.getBoundaryParameterOption().setSelected(BoundaryParameterOption.Type.FLOW_DIRECTION_X);
                       break; 
                   case "FLOW_DIRECTION_Y":
                       boundarySensitivitiesReport.getBoundaryParameterOption().setSelected(BoundaryParameterOption.Type.FLOW_DIRECTION_Y);
                       break; 
                   case "FLOW_DIRECTION_Z":
                       boundarySensitivitiesReport.getBoundaryParameterOption().setSelected(BoundaryParameterOption.Type.FLOW_DIRECTION_Z);
                       break; 
           }
           boundarySensitivitiesReport.getParts().setQuery(null);
           Region region = m_simulation.getRegionManager().getRegion(regionName);
           Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
           boundarySensitivitiesReport.getParts().setObjects(boundary);
           boundarySensitivitiesReport.setPresentationName(reportName);
           m_simulation.getMonitorManager().createMonitorAndPlot(new NeoObjectVector(new Object[] {boundarySensitivitiesReport}), true, "%1$s Plot");
           ReportMonitor reportMonitor = ((ReportMonitor) m_simulation.getMonitorManager().getMonitor(reportName + " Monitor"));
           MonitorPlot monitorPlot = m_simulation.getPlotManager().createMonitorPlot(new NeoObjectVector(new Object[] {reportMonitor}), reportName + " Monitor Plot");
        }
    }
    
    // This method creates a CAD model object 
    public CadModel createCADModel(String cadModelName){
       SolidModelManager solidModelManager = m_simulation.get(SolidModelManager.class);
       Scene cadScene = m_simulation.getSceneManager().createScene(cadModelName);
       cadScene.initializeAndWait();
       return solidModelManager.createSolidModel(cadScene); 
    }
    
    //  This method creates a derived threshold part 
    public void createDerivedPartThreshold(String partName, String fieldFunctionName, String regionName){
        Units units_0 = ((Units) m_simulation.getUnitsManager().getObject("m"));
        NullFieldFunction nullFieldFunction_0 = ((NullFieldFunction) m_simulation.getFieldFunctionManager().getFunction("NullFieldFunction"));
        ThresholdPart thresholdPart_3 = m_simulation.getPartManager().createThresholdPart(new NeoObjectVector(new Object[] {}), new DoubleVector(new double[] {-0.5, 0.5}), units_0, nullFieldFunction_0, 0);
        PrimitiveFieldFunction primitiveFieldFunction_0 = ((PrimitiveFieldFunction) m_simulation.getFieldFunctionManager().getFunction(fieldFunctionName));
        thresholdPart_3.setFieldFunction(primitiveFieldFunction_0);
        thresholdPart_3.getInputParts().setQuery(null);
        Region region_0 = m_simulation.getRegionManager().getRegion(regionName);
        thresholdPart_3.getInputParts().setObjects(region_0);
        thresholdPart_3.setPresentationName(partName); 
    }
    
    // This method creates a scalar parameter for flow angles (alpha, beta)
    public void createGlobalScalarParameter(String parameterName, double value){
        createGlobalScalarParameter(parameterName, value, " ");
    }
    public void createGlobalScalarParameter(String parameterName, double value, String units) {
        IntVector vec = new IntVector(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        switch (units){
            case "deg": 
                vec = new IntVector(new int[] {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                break;
            case "m/s":
                vec = new IntVector(new int[] {0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                break; 
            case "K":
                vec = new IntVector(new int[] {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                break;
            case "Pa":
                vec = new IntVector(new int[] {0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                break;
            case "N.s/m2":
                vec = new IntVector(new int[] {0, -2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                break;
            case "kg/m3":
                vec = new IntVector(new int[] {1, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                break;
            // add other units here (place holder)
        }
      if (!m_simulation.get(GlobalParameterManager.class).has(parameterName)){
            m_simulation.get(GlobalParameterManager.class).createGlobalParameter(ScalarGlobalParameter.class, "Scalar");
            ScalarGlobalParameter scalarGlobalParameter_0 = ((ScalarGlobalParameter) m_simulation.get(GlobalParameterManager.class).getObject("Scalar"));
            scalarGlobalParameter_0.setPresentationName(parameterName);
            scalarGlobalParameter_0.getQuantity().setValue(value);
            scalarGlobalParameter_0.setDimensionsVector(vec); // angle
            if (units.equals("deg")){
                Units units_0 = ((Units) m_simulation.getUnitsManager().getObject("deg"));
                scalarGlobalParameter_0.getQuantity().setUnits(units_0);
            }
      }
//      else {
//            ScalarGlobalParameter scalarGlobalParameter = ((ScalarGlobalParameter) m_simulation.get(GlobalParameterManager.class).getObject(parameterName));
//            scalarGlobalParameter.getQuantity().setValue(value);
//      }
    }
    
    // This method creates a scalar parameter for flow angles (alpha, beta)
    public void updateGlobalScalarParameter(String parameterName, double value) {
        ScalarGlobalParameter scalarGlobalParameter = ((ScalarGlobalParameter) m_simulation.get(GlobalParameterManager.class).getObject(parameterName));
        scalarGlobalParameter.getQuantity().setValue(value);
    }
      
    // This method creates a force coefficient report and monitor plot
    public void createForceCoefReport(   String reportName,
                                         String regionName,
                                         ArrayList<String> boundaryNames,
                                         double[] flowDirection,
                                         double refPressure,
                                         double refDensity,
                                         double refVelocity,
                                         double refArea, 
                                         boolean isEuler)
    {
        // Create report
        ForceCoefficientReport forceCoefficientReport = m_simulation.getReportManager().createReport(ForceCoefficientReport.class);
        forceCoefficientReport.setPresentationName(reportName);
        forceCoefficientReport.getDirection().setComponents(flowDirection[0], flowDirection[1], flowDirection[2]);
        forceCoefficientReport.getReferencePressure().setValue(refPressure);
        forceCoefficientReport.getReferenceDensity().setValue(refDensity);
        forceCoefficientReport.getReferenceVelocity().setValue(refVelocity);
        forceCoefficientReport.getReferenceArea().setValue(refArea);
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Collection<Boundary> objects = new NeoObjectVector(new Object[] {});
        for (int i = 0; i < boundaryNames.size(); i++){
            Boundary boundary = region.getBoundaryManager().getBoundary(boundaryNames.get(i));
            objects.add(boundary);
        }
        forceCoefficientReport.getParts().setObjects(objects);
        forceCoefficientReport.setRepresentation(null);
        if (isEuler){forceCoefficientReport.getForceOption().setSelected(ForceReportForceOption.Type.PRESSURE);}
        
        // Create monitor and plot too
        m_simulation.getMonitorManager().createMonitorAndPlot(new NeoObjectVector(new Object[] {forceCoefficientReport}), true, "%1$s Plot");
        ReportMonitor reportMonitor = ((ReportMonitor) m_simulation.getMonitorManager().getMonitor(reportName + " Monitor"));
        MonitorPlot monitorPlot = m_simulation.getPlotManager().createMonitorPlot(new NeoObjectVector(new Object[] {reportMonitor}), reportName + " Monitor Plot");
    }
    
    // This method creates a cost function from a specified force report.
    public void createForceCostFunction(String forceReportName, String costFunctionName){
        ManagerManager managerManager = m_simulation.getManagerManager();
        AdjointCostFunctionManager adjointCostFunctionManager = managerManager.has(AdjointCostFunctionManager.class); 
        try {
//            if (adjointCostFunctionManager.has(forceReportName)){
//                AdjointCostFunction costFunction = adjointCostFunctionManager.getAdjointCostFunction(forceReportName); 
//                adjointCostFunctionManager.remove(costFunction);
//            }
            if (!adjointCostFunctionManager.has(forceReportName)){
                ForceCostFunction forceCostFunction = m_simulation.get(AdjointCostFunctionManager.class).createAdjointCostFunction(ForceCostFunction.class);
                ForceReport forceReport = ((ForceReport) m_simulation.getReportManager().getReport(forceReportName));
                forceCostFunction.setForceReport(forceReport);
                forceCostFunction.setPresentationName(costFunctionName);
            }
        }
        catch (Exception e){print(e.getMessage());}
    }
    
    // This method creates an expression report that has units of force
    public void createForceExpressionReport(String reportName, String expression) {
        if (m_simulation.getReportManager().has(reportName)){
            Report report = m_simulation.getReportManager().getObject(reportName);
            m_simulation.getReportManager().remove(report); 
        }
        ExpressionReport expressionReport_0 = m_simulation.getReportManager().createReport(ExpressionReport.class);
        expressionReport_0.setDefinition(expression);
        expressionReport_0.setDimensionsVector(new IntVector(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        expressionReport_0.setPresentationName(reportName);
    }
    
    // This method creates a table for the gradient of an existing adjoint cost function 
    // ------------------- starccm+ v11.04.12 ----------------------------------
//    public void createGradientTable(String adjointCostFunctionName, ArrayList<String> controlPointObjectNames) {
//        XyzInternalTable xyzInternalTable_0 = m_simulation.getTableManager().createTable(XyzInternalTable.class);
//        for (String controlPointObjectName: controlPointObjectNames){
//             ControlPointRegion controlPointRegion = ((ControlPointRegion) m_simulation.get(ControlPointRegionManager.class).getObject(controlPointObjectName));
//             xyzInternalTable_0.getParts().setObjects(controlPointRegion);
//             ForceCostFunction costFunction = (ForceCostFunction) m_simulation.getAdjointCostFunctionManager().getObject(adjointCostFunctionName);
//             int ID = costFunction.getID(); 
//             PrimitiveFieldFunction primitiveFieldFunction_0 = ((PrimitiveFieldFunction) m_simulation.getFieldFunctionManager().getFunction("Adjoint" + ID + "::CoordAdjoint"));
//             VectorComponentFieldFunction vectorComponentFieldFunction_0 = ((VectorComponentFieldFunction) primitiveFieldFunction_0.getComponentFunction(0));
//             VectorComponentFieldFunction vectorComponentFieldFunction_1 = ((VectorComponentFieldFunction) primitiveFieldFunction_0.getComponentFunction(1));
//             VectorComponentFieldFunction vectorComponentFieldFunction_2 = ((VectorComponentFieldFunction) primitiveFieldFunction_0.getComponentFunction(2));
//             xyzInternalTable_0.setFieldFunctions(new NeoObjectVector(new Object[] {vectorComponentFieldFunction_0, vectorComponentFieldFunction_1, vectorComponentFieldFunction_2}));
//             xyzInternalTable_0.setPresentationName("Gradient of " + adjointCostFunctionName + " w.r.t. " + controlPointObjectName);
//             xyzInternalTable_0.extract();
//        }
//    }
    // -------------------------------------------------------------------------
    public void createGradientTable(String adjointCostFunctionName, ArrayList<String> controlPointObjectNames) {
        XyzInternalTable xyzInternalTable_0 = m_simulation.getTableManager().createTable(XyzInternalTable.class);
        for (String controlPointObjectName: controlPointObjectNames){
//             PointSet controlPointRegion = ((PointSet) m_simulation.get(PointSetManager.class).getObject(controlPointObjectName));
             PointSet controlPointRegion = ((PointSet) m_simulation.get(PointSetManager.class).getObject(controlPointObjectName));
             xyzInternalTable_0.getParts().setObjects(controlPointRegion);
             int ID = 0; 
             if (getCostFunctionType(adjointCostFunctionName).equals("Force")){
                ForceCostFunction costFunction = (ForceCostFunction) m_simulation.getAdjointCostFunctionManager().getObject(adjointCostFunctionName);
                ID = costFunction.getID(); 
             }
             if (getCostFunctionType(adjointCostFunctionName).equals("Moment")){
                MomentCostFunction costFunction = (MomentCostFunction) m_simulation.getAdjointCostFunctionManager().getObject(adjointCostFunctionName);
                ID = costFunction.getID(); 
             }
             PrimitiveFieldFunction primitiveFieldFunction_0 = ((PrimitiveFieldFunction) m_simulation.getFieldFunctionManager().getFunction("Adjoint" + ID + "::CoordAdjoint"));
             VectorComponentFieldFunction vectorComponentFieldFunction_0 = ((VectorComponentFieldFunction) primitiveFieldFunction_0.getComponentFunction(0));
             VectorComponentFieldFunction vectorComponentFieldFunction_1 = ((VectorComponentFieldFunction) primitiveFieldFunction_0.getComponentFunction(1));
             VectorComponentFieldFunction vectorComponentFieldFunction_2 = ((VectorComponentFieldFunction) primitiveFieldFunction_0.getComponentFunction(2));
             xyzInternalTable_0.setFieldFunctions(new NeoObjectVector(new Object[] {vectorComponentFieldFunction_0, vectorComponentFieldFunction_1, vectorComponentFieldFunction_2}));
             xyzInternalTable_0.setPresentationName("Gradient of " + adjointCostFunctionName + " w.r.t. " + controlPointObjectName);
             xyzInternalTable_0.extract();
        }
    }
    
    // This method creates a force coefficient report and monitor plot
    public void createMomentCoefReport( String reportName,
                                        String regionName,
                                        ArrayList<String> boundaryNames,
                                        double[] axis,
                                        double[] origin, 
                                        double refPressure,
                                        double refDensity,
                                        double refVelocity,
                                        double refArea, 
                                        double refRadius,
                                        boolean isEuler)
    {
        // Create report
        MomentCoefficientReport momentCoefficientReport = m_simulation.getReportManager().createReport(MomentCoefficientReport.class);
        momentCoefficientReport.setPresentationName(reportName);
        momentCoefficientReport.getDirection().setComponents(axis[0], axis[1], axis[2]);
        momentCoefficientReport.getReferencePressure().setValue(refPressure);
        momentCoefficientReport.getReferenceDensity().setValue(refDensity);
        momentCoefficientReport.getReferenceVelocity().setValue(refVelocity);
        momentCoefficientReport.getReferenceArea().setValue(refArea);
        momentCoefficientReport.getOrigin().setComponents(origin[0], origin[1], origin[2]);
        momentCoefficientReport.getReferenceRadius().setValue(refRadius);
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Collection<Boundary> objects = new NeoObjectVector(new Object[] {});
        for (int i = 0; i < boundaryNames.size(); i++){
            Boundary boundary = region.getBoundaryManager().getBoundary(boundaryNames.get(i));
            objects.add(boundary);
        }
        momentCoefficientReport.getParts().setObjects(objects);
        momentCoefficientReport.setRepresentation(null);
        if (isEuler){momentCoefficientReport.getForceOption().setSelected(ForceReportForceOption.Type.PRESSURE);}
        
        // Create monitor and plot too
        m_simulation.getMonitorManager().createMonitorAndPlot(new NeoObjectVector(new Object[] {momentCoefficientReport}), true, "%1$s Plot");
        ReportMonitor reportMonitor = ((ReportMonitor) m_simulation.getMonitorManager().getMonitor(reportName + " Monitor"));
        MonitorPlot monitorPlot = m_simulation.getPlotManager().createMonitorPlot(new NeoObjectVector(new Object[] {reportMonitor}), reportName + " Monitor Plot");
    }
    
    // This method creates a cost function from a specified force report.
    public void createMomentCostFunction(String momentReportName, String costFunctionName){
        ManagerManager managerManager = m_simulation.getManagerManager();
        AdjointCostFunctionManager adjointCostFunctionManager = managerManager.has(AdjointCostFunctionManager.class); 
        try {
//            if (adjointCostFunctionManager.has(forceReportName)){
//                AdjointCostFunction costFunction = adjointCostFunctionManager.getAdjointCostFunction(forceReportName); 
//                adjointCostFunctionManager.remove(costFunction);
//            }
            if (!adjointCostFunctionManager.has(momentReportName)){
                MomentCostFunction momentCostFunction = m_simulation.get(AdjointCostFunctionManager.class).createAdjointCostFunction(MomentCostFunction.class);
                MomentReport momentReport = ((MomentReport) m_simulation.getReportManager().getReport(momentReportName));
                momentCostFunction.setMomentReport(momentReport);
                momentCostFunction.setPresentationName(costFunctionName);
            }
        }
        catch (Exception e){print(e.getMessage());}
    }
    
    // This method assigns an existing part to a new region 
    public void createRegion(String partName, String regionName){
        Region region = m_simulation.getRegionManager().createEmptyRegion();
        region.setPresentationName(regionName);
        Boundary boundary = region.getBoundaryManager().getBoundary("Default");
        region.getBoundaryManager().removeBoundaries(new NeoObjectVector(new Object[] {boundary}));
        FeatureCurve featureCurve_2 = ((FeatureCurve) region.getFeatureCurveManager().getObject("Default"));
        region.getFeatureCurveManager().removeObjects(featureCurve_2);
        FeatureCurve featureCurve_3 =  region.getFeatureCurveManager().createEmptyFeatureCurveWithName("Feature Curve");
        MeshPart meshPart_0 = ((MeshPart) m_simulation.get(SimulationPartManager.class).getPart(partName));
        m_simulation.getRegionManager().newRegionsFromParts(new NeoObjectVector(new Object[] {meshPart_0}), "OneRegion", region, "OneBoundaryPerPartSurface", null, "OneFeatureCurve", featureCurve_3, RegionManager.CreateInterfaceMode.BOUNDARY);
    }
    
    // This method creates a sum report 
    public void createSumReport(String reportName, String partName, String fieldFunctionName){ 
        SumReport sumReport_0 = m_simulation.getReportManager().createReport(SumReport.class);
        sumReport_0.setPresentationName(reportName);
        ThresholdPart thresholdPart_1 = ((ThresholdPart) m_simulation.getPartManager().getObject(partName));
        sumReport_0.getParts().setObjects(thresholdPart_1);
        PrimitiveFieldFunction primitiveFieldFunction_1 = ((PrimitiveFieldFunction) m_simulation.getFieldFunctionManager().getFunction(fieldFunctionName));
        sumReport_0.setFieldFunction(primitiveFieldFunction_1);
    }
    
    // This method creates a scalar field function, where the definition is the 
    // expression one would write using the GUI. For example: 
    // String fieldFunctionDefinition = "1.2*pow(${Volume},0.5)";
    public void createUserFieldFunction(String fieldFunctionName,String fieldFunctionDefinition) {
        UserFieldFunction userFieldFunction_0 = m_simulation.getFieldFunctionManager().createFieldFunction();
        userFieldFunction_0.getTypeOption().setSelected(FieldFunctionTypeOption.Type.SCALAR);
        userFieldFunction_0.setFunctionName(fieldFunctionName);
        userFieldFunction_0.setPresentationName(fieldFunctionName);
        userFieldFunction_0.setDefinition(fieldFunctionDefinition);
    }
    
    // This method creates a volume report
    public void createVolumeReport(String reportName, String partName, String fieldFunctionName) {

        VolumeAverageReport volumeAverageReport_0 =  m_simulation.getReportManager().createReport(VolumeAverageReport.class);
        volumeAverageReport_0.setPresentationName(reportName);
        ThresholdPart thresholdPart_2 = ((ThresholdPart) m_simulation.getPartManager().getObject(partName));
        volumeAverageReport_0.getParts().setObjects(thresholdPart_2);
        UserFieldFunction userFieldFunction_16 = ((UserFieldFunction) m_simulation.getFieldFunctionManager().getFunction(fieldFunctionName));
        volumeAverageReport_0.setFieldFunction(userFieldFunction_16);
    }

    public void createElementCountReport(String reportName, String regionName){

        ElementCountReport elementCountReport_0 = m_simulation.getReportManager().createReport(ElementCountReport.class);
        elementCountReport_0.setPresentationName(reportName);
        elementCountReport_0.getParts().setObjects(m_simulation.getRegionManager().getRegion(regionName));

    }

    // This method creates an xyz table 
    public void createXyzTable(String tableName, String partName, String fieldFunctionName){
        XyzInternalTable xyzInternalTable_1 = m_simulation.getTableManager().createTable(XyzInternalTable.class);
        UserFieldFunction userFieldFunction_17 = ((UserFieldFunction) m_simulation.getFieldFunctionManager().getFunction(fieldFunctionName));
        xyzInternalTable_1.setFieldFunctions(new NeoObjectVector(new Object[] {userFieldFunction_17}));
        xyzInternalTable_1.setPresentationName(tableName);
        Part part = m_simulation.getPartManager().getObject(partName);
        xyzInternalTable_1.getParts().setObjects(part);
    }
    
    // ---------------------- customize ----------------------------------------
    
    // This method customizes the minimum surface size of a surface. These com-
    // mands were obtained by reccording a macro in Star-CCM+. 
    public void customizeMinimumSurfaceSize2D(String meshOperationName,String partName,String partSurfaceName,double size){
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        if (!autoMeshOperation2d.getCustomMeshControls().has(partSurfaceName)){
             createSurfaceCustomMeshControl2D(meshOperationName,partName,partSurfaceName);
        }
       SurfaceCustomMeshControl surfaceCustomMeshControl = ((SurfaceCustomMeshControl) autoMeshOperation2d.getCustomMeshControls().getObject(partSurfaceName));
       surfaceCustomMeshControl.getCustomConditions().get(PartsMinimumSurfaceSizeOption.class).setSelected(PartsMinimumSurfaceSizeOption.Type.CUSTOM);
       PartsMinimumSurfaceSize partsMinimumSurfaceSize = surfaceCustomMeshControl.getCustomValues().get(PartsMinimumSurfaceSize.class);
       partsMinimumSurfaceSize.getRelativeOrAbsoluteOption().setSelected(RelativeOrAbsoluteOption.Type.ABSOLUTE);
       GenericAbsoluteSize minimumAbsoluteSize = ((GenericAbsoluteSize) partsMinimumSurfaceSize.getAbsoluteSize());
       minimumAbsoluteSize.getValue().setValue(size);
    }
    
    // Same method as above, but for 3D
    public void customizeMinimumSurfaceSize3D(String meshOperationName,String partName,String partSurfaceName,double size){
        // TO DO
    }
    
   // This method sets the number of prism layers on a given surface. This 
    // method was reccorded in Starccm+. 
    public void customizePrismLayerNumber2D(String meshOperationName,String partName,String partSurfaceName,int numberLayers){ 
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        if (!autoMeshOperation2d.getCustomMeshControls().has(partSurfaceName)){
             createSurfaceCustomMeshControl2D(meshOperationName,partName,partSurfaceName);
        }
        SurfaceCustomMeshControl surfaceCustomMeshControl = ((SurfaceCustomMeshControl) autoMeshOperation2d.getCustomMeshControls().getObject(partSurfaceName));
        PartsCustomizePrismMesh partsCustomizePrismMesh = surfaceCustomMeshControl.getCustomConditions().get(PartsCustomizePrismMesh.class);
        partsCustomizePrismMesh.getCustomPrismOptions().setSelected(PartsCustomPrismsOption.Type.CUSTOMIZE);
        PartsCustomizePrismMeshControls partsCustomizePrismMeshControls = partsCustomizePrismMesh.getCustomPrismControls();
        partsCustomizePrismMeshControls.setCustomizeNumLayers(true);
        NumPrismLayers numPrismLayers = surfaceCustomMeshControl.getCustomValues().get(CustomPrismValuesManager.class).get(NumPrismLayers.class);
        numPrismLayers.setNumLayers(numberLayers);
    }
    
    // Same method as above, but for 3D
    public void customizePrismLayerNumber3D(String meshOperationName,String partName,String partSurfaceName,int numberLayers){
        // TO DO
    }

   // This method sets the prism layer total thickness on a given surface. This 
    // method was reccorded in Starccm+.  
    public void customizePrismLayerTotalThickness2D(String meshOperationName,String partName,String partSurfaceName, double totalThickness){
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        if (!autoMeshOperation2d.getCustomMeshControls().has(partSurfaceName)){
             createSurfaceCustomMeshControl2D(meshOperationName,partName,partSurfaceName);
        }
       SurfaceCustomMeshControl surfaceCustomMeshControl = ((SurfaceCustomMeshControl) autoMeshOperation2d.getCustomMeshControls().getObject(partSurfaceName));
       PartsCustomizePrismMesh partsCustomizePrismMesh = surfaceCustomMeshControl.getCustomConditions().get(PartsCustomizePrismMesh.class);
       partsCustomizePrismMesh.getCustomPrismOptions().setSelected(PartsCustomPrismsOption.Type.CUSTOMIZE);
       PartsCustomizePrismMeshControls partsCustomizePrismMeshControls = partsCustomizePrismMesh.getCustomPrismControls();
       partsCustomizePrismMeshControls.setCustomizeTotalThickness(true);
       PrismThickness prismThickness = surfaceCustomMeshControl.getCustomValues().get(CustomPrismValuesManager.class).get(PrismThickness.class);
       prismThickness.getRelativeOrAbsoluteOption().setSelected(RelativeOrAbsoluteOption.Type.ABSOLUTE);
       GenericAbsoluteSize genericAbsoluteSize = ((GenericAbsoluteSize) prismThickness.getAbsoluteSize());
       genericAbsoluteSize.getValue().setValue(totalThickness);
    }
    
    // Same method as above, but for 3D
    public void customizePrismLayerTotalThickness3D(String meshOperationName,String partName,String partSurfaceName, double totalThickness){
        // TO DO
    }
    
    // This method sets the prism layer wall thickness on a given surface. This 
    // method was reccorded in Starccm+.  
    public void customizePrismLayerWallThickness2D(String meshOperationName,String partName,String partSurfaceName, double wallThickness){
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        if (!autoMeshOperation2d.getCustomMeshControls().has(partSurfaceName)){
             createSurfaceCustomMeshControl2D(meshOperationName,partName,partSurfaceName);
        }
        SurfaceCustomMeshControl surfaceCustomMeshControl = ((SurfaceCustomMeshControl) autoMeshOperation2d.getCustomMeshControls().getObject(partSurfaceName));
        PartsCustomizePrismMesh partsCustomizePrismMesh = surfaceCustomMeshControl.getCustomConditions().get(PartsCustomizePrismMesh.class);
        PartsCustomizePrismMeshControls partsCustomizePrismMeshControls = partsCustomizePrismMesh.getCustomPrismControls();
        partsCustomizePrismMeshControls.setCustomizeStretching(true);
        surfaceCustomMeshControl.getCustomValues().get(CustomPrismValuesManager.class).get(PrismWallThickness.class).setValue(wallThickness);
    }
    
    // Same method as above, but for 3D
    public void customizePrismLayerWallThickness3D(String meshOperationName,String partName,String partSurfaceName, double wallThickness){
        // TO DO
    }
    
    // This method customizes the target surface size of a surface. These com-
    // mands were obtained by reccording a macro in Star-CCM+ and modifying them. 
    public void customizeTargetSurfaceSize2D(String meshOperationName,String partName, String partSurfaceName,double size){
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        if (!autoMeshOperation2d.getCustomMeshControls().has(partSurfaceName)){
             createSurfaceCustomMeshControl2D(meshOperationName,partName,partSurfaceName);
        }
        SurfaceCustomMeshControl surfaceCustomMeshControl = ((SurfaceCustomMeshControl) autoMeshOperation2d.getCustomMeshControls().getObject(partSurfaceName));
        surfaceCustomMeshControl.getCustomConditions().get(PartsTargetSurfaceSizeOption.class).setSelected(PartsTargetSurfaceSizeOption.Type.CUSTOM);
        PartsTargetSurfaceSize partsTargetSurfaceSize = surfaceCustomMeshControl.getCustomValues().get(PartsTargetSurfaceSize.class);
        partsTargetSurfaceSize.getRelativeOrAbsoluteOption().setSelected(RelativeOrAbsoluteOption.Type.ABSOLUTE);
        GenericAbsoluteSize genericAbsoluteSize = ((GenericAbsoluteSize) partsTargetSurfaceSize.getAbsoluteSize());
        genericAbsoluteSize.getValue().setValue(size);
    }
    
    // Same method as above, but for 3D
    public void customizeTargetSurfaceSize3D(String meshOperationName,String partName,String partSurfaceName,double size){
        // TO DO
    }
    
    // ---------------------- deletion -----------------------------------------
    
    // This method deletes adjoint cost functions 
    public void deleteAdjointCostFunctions(){
       ManagerManager managerManager = m_simulation.getManagerManager();
       AdjointCostFunctionManager adjointCostFunctionManager = managerManager.has(AdjointCostFunctionManager.class); 
       try {
           Collection<AdjointCostFunction> objects = adjointCostFunctionManager.getObjects(); 
           adjointCostFunctionManager.removeObjects(objects); 
           managerManager.remove(adjointCostFunctionManager); 
       } 
       catch (NullPointerException e){
           // Do nothing - just means there were no objects to delete
       }
    }
    
    // This method deletes an adjoint cost function 
    public void deleteAdjointCostFunction(String name){
       ManagerManager managerManager = m_simulation.getManagerManager();
       AdjointCostFunctionManager adjointCostFunctionManager = managerManager.has(AdjointCostFunctionManager.class); 
       try {
           AdjointCostFunction object = adjointCostFunctionManager.getAdjointCostFunction(name); 
           adjointCostFunctionManager.removeObjects(object); 
       } 
       catch (NullPointerException e){
           // Do nothing - just means there were no objects to delete
       }
    }

    // This method deletes all cad models 
    public void deleteCADModels(){
       SolidModelManager solidModelManager = m_simulation.get(SolidModelManager.class);
       Collection<CadModelBase> objects = solidModelManager.getObjects(); 
       solidModelManager.removeObjects(objects); 
    }
    
     // This method deletes all meshes 
     public void deleteContinua(){
        ContinuumManager continuumManager = m_simulation.getContinuumManager();
        Collection<Continuum> objects = continuumManager.getObjects(); 
        continuumManager.removeObjects(objects);  
     }

    // This method deletes control points 
    // ------------------- starccm+ v11.04.12 ---------------------------------- 
    public void deleteControlPoints(){
       ManagerManager managerManager = m_simulation.getManagerManager();
       PointSetManager pointSetManager = managerManager.has(PointSetManager.class); 
       try {
           Collection<PointSet> objects = pointSetManager.getObjects(); 
           pointSetManager.removeObjects(objects); 
       } 
       catch (NullPointerException e){
           // Do nothing - just means there were no objects to delete
       }
    }
    // -------------------------------------------------------------------------  
//    public void deleteControlPoints(){
//       ManagerManager managerManager = m_simulation.getManagerManager();
//       PointSetManager controlPointRegionManager = managerManager.has(PointSetManager.class); 
//       try {
//           Collection<PointSet> objects = controlPointRegionManager.getObjects(); 
//           controlPointRegionManager.removeObjects(objects); 
//       } 
//       catch (NullPointerException e){
//           // Do nothing - just means there were no objects to delete
//       }
//    }
    
    // This method deletes a derived part 
    public void deleteDerivedPart(String partName){
        PartManager partManager = m_simulation.getPartManager(); 
        if (partManager.has(partName)){
            ThresholdPart thresholdPart_0 = ((ThresholdPart) m_simulation.getPartManager().getObject(partName));
            m_simulation.getPartManager().removeObjects(thresholdPart_0);
        }
    }
   
    // This method deletes selected meshers
    public void deleteMesher(String meshOperationName){
        ManagerManager managerManager = m_simulation.getManagerManager();
        MeshOperationManager meshOperationManager = managerManager.has(MeshOperationManager.class);
        if (!(meshOperationManager == null)){
            if (meshOperationManager.has(meshOperationName)){
                MeshOperation meshOperation = meshOperationManager.getObject(meshOperationName);
                meshOperationManager.erase(meshOperation);
            }
        }
    }    

    // This method deletes selected meshers
    public void deleteMeshers(){
        ManagerManager managerManager = m_simulation.getManagerManager();
        MeshOperationManager meshOperationManager = managerManager.has(MeshOperationManager.class);
        if (!(meshOperationManager == null)){
            Collection<MeshOperation> objects = meshOperationManager.getObjects();
            meshOperationManager.removeObjects(objects);
        }
    }
    
    // This method deletes all meshes
    public void deleteMeshes(){
        ManagerManager managerManager = m_simulation.getManagerManager();
        MeshPipelineController meshPipelineController = managerManager.has(MeshPipelineController.class);
        if (!(meshPipelineController == null)){
            meshPipelineController.clearGeneratedMeshes();
        }
    }
     
     // This method deletes all monitors 
     public void deleteMonitors(){
        ManagerManager managerManager = m_simulation.getManagerManager();
        MonitorManager monitorManager = managerManager.has(MonitorManager.class);
        if (!(monitorManager == null)){
            Collection<Monitor> objects = monitorManager.getObjects();
            // These can't be deleted once created, so remove from collection
            if (monitorManager.has("Iteration")){
                objects.remove(monitorManager.getObject("Iteration"));
            }
            if (monitorManager.has("Physical Time")){
                objects.remove(monitorManager.getObject("Physical Time"));
            }
            monitorManager.removeObjects(objects); 
        }
     }
    
     // This method deletes all parts 
     public void deleteParts(){
        SimulationPartManager simulationPartManager = m_simulation.get(SimulationPartManager.class);
        Collection<GeometryPart> objects = simulationPartManager.getObjects(); 
        simulationPartManager.removeObjects(objects); 
     }
     
     // This method deletes all meshes 
     public void deletePhysicsContinuum(String physicsContinuumName){
        ContinuumManager continuumManager = m_simulation.getContinuumManager();
        if (continuumManager.has(physicsContinuumName)){
            PhysicsContinuum object = ((PhysicsContinuum) continuumManager.getContinuum(physicsContinuumName));
            continuumManager.removeObjects(object);  
        }
     }
    
    // This method deletes all plots  
     public void deletePlots(){
        PlotManager plotManager = m_simulation.getPlotManager(); 
        Collection<StarPlot> objects = plotManager.getObjects();
        if (plotManager.has("Residuals")){
            objects.remove(plotManager.getObject("Residuals"));
        }
        plotManager.removeObjects(objects); 
     }
    
    public void deleteReport(String reportName) {
        ReportManager reportManager = m_simulation.getReportManager(); 
        if (reportManager.has(reportName)){
            Report object = reportManager.getReport(reportName);
            reportManager.removeObjects(object);
        }
    }
    
    // This method deletes all regions 
     public void deleteRegion(String regionName){
        RegionManager regionManager = m_simulation.get(RegionManager.class);
        if (regionManager.has(regionName)){
            Region object = regionManager.getObject(regionName);
            regionManager.removeObjects(object);
        }  
     }
    
    // This method deletes all regions 
     public void deleteRegions(){
        RegionManager regionManager = m_simulation.get(RegionManager.class);
        Collection<Region> objects = regionManager.getObjects(); 
        regionManager.removeObjects(objects);  
     }
     
    // This method deletes all reports 
    public void deleteReports(){
       ReportManager reportManager = m_simulation.getReportManager(); 
       Collection<Report> objects = reportManager.getObjects(); 
       reportManager.removeObjects(objects); 
    }
     
    // This method deletes all scenes 
    public void deleteScenes(){
       SceneManager sceneManager = m_simulation.get(SceneManager.class);
       Collection<Scene> objects = sceneManager.getObjects(); 
       sceneManager.removeObjects(objects);  
    }
    
    // This method deletes a table
    public void deleteTable(String tableName){
        TableManager tableManager = m_simulation.getTableManager(); 
        if (tableManager.has(tableName)){
            Table object = tableManager.getTable(tableName);
            try {tableManager.removeObjects(object);}
            catch (Exception e){print(e.getMessage());}
        }
    }
    
    // This method deletes all tables 
     public void deleteTables(){
        TableManager tableManager = m_simulation.getTableManager(); 
        Collection<Table> objects = tableManager.getObjects(); 
        for (Table object: objects){
            try {tableManager.remove(object);}
            catch (Exception e){print(e.getMessage());}
        } 
     }
    
    // This method creates a scalar field function
    public void deleteUserFieldFunction(String fieldFunctionName) {
        FieldFunctionManager fieldFunctionManager = m_simulation.getFieldFunctionManager();
        if (fieldFunctionManager.has(fieldFunctionName)){
            UserFieldFunction userFieldFunction = ((UserFieldFunction) fieldFunctionManager.getFunction(fieldFunctionName));
            m_simulation.getFieldFunctionManager().removeObjects(userFieldFunction);
        }
    }   
    
    // ---------------------- disabling ----------------------------------------
    
    // This method disables the prism layer on the specified boundary
    public void disablePrismLayer2D(String meshOperationName,String partName,String partSurfaceName){
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        if (!autoMeshOperation2d.getCustomMeshControls().has(partSurfaceName)){
             createSurfaceCustomMeshControl2D(meshOperationName,partName,partSurfaceName);
        }
       SurfaceCustomMeshControl surfaceCustomMeshControl = ((SurfaceCustomMeshControl) autoMeshOperation2d.getCustomMeshControls().getObject(partSurfaceName));
       PartsCustomizePrismMesh partsCustomizePrismMesh = surfaceCustomMeshControl.getCustomConditions().get(PartsCustomizePrismMesh.class);
       partsCustomizePrismMesh.getCustomPrismOptions().setSelected(PartsCustomPrismsOption.Type.DISABLE);
    }
    
    // Same method as above, but for 3D
    public void disablePrismLayer3D(String meshOperationName,String partName,String partSurfaceName){
        // TO DO
    }
    
    // ---------------------- getting ------------------------------------------
    
    // --- This method gets the cost function report ID 
    public int getCostFunctionID(String functionName){
        AdjointCostFunctionManager adjointCostFunctionManager = m_simulation.getAdjointCostFunctionManager();
        AdjointCostFunction costFunction = adjointCostFunctionManager.getAdjointCostFunction(functionName);
        return costFunction.getID();     
    }
    
    // --- This method returns the report type (force or moment) 
    public String getCostFunctionType(String functionName){
        AdjointCostFunctionManager adjointCostFunctionManager = m_simulation.getAdjointCostFunctionManager();
        AdjointCostFunction costFunction = adjointCostFunctionManager.getAdjointCostFunction(functionName);
        Class c = costFunction.getClass();   
        String type = null; 
        if (c.equals(MomentCostFunction.class)){
            type = "Moment";
        }
        if (c.equals(ForceCostFunction.class)){
            type = "Force";
        }
        return type; 
    }
    
    // This method returns the data contained in the specified gradient table
    public TableData getGradientTableData(String functionName, String controlPointTableName){
       return getTableData("Gradient of " + functionName + " w.r.t. " + controlPointTableName); 
    } 
    
    // --- This method returns the report type (force or moment) 
    public String getReportType(String reportName){
        ReportManager reportManager = m_simulation.getReportManager();
        Report report = (Report) reportManager.getReport(reportName);
        Class c = report.getClass();   
        String type = null; 
        if (c.equals(MomentReport.class) | c.equals(MomentCoefficientReport.class)){
            type = "Moment";
        }
        if (c.equals(ForceReport.class) | c.equals(ForceCoefficientReport.class)){
            type = "Force";
        }
        return type; 
    }
    
   // This method returns the value from an analysis report
    public double getReportValue(String reportName){
       ReportManager reportManager = m_simulation.getReportManager();
       Report report = (Report) reportManager.getReport(reportName);
       return report.getReportMonitorValue();
    }
    
    // This method returns the simulation name  
    public String getSimulationName(){return m_simulation.getPresentationName();}
  
    // This method returns the data contained in the specified table name
    public TableData getTableData(String tableName){
       TableManager tableManager = m_simulation.getTableManager();
       Table table = tableManager.getTable(tableName);
       table.extract();
       return table.getSeriesData(); 
    } 
    
    // ---------------------- printing -----------------------------------------
    
    // These methods print to the Starccm+ output window
    public void print(String s){m_simulation.println(s);}
    public void print(double d){m_simulation.println(d);}
    public void print(int i){m_simulation.println(i);}
    public void print(boolean b){m_simulation.println(b);}
    
    // ---------------------- renaming -----------------------------------------
    
     // This method renames a CAD model
     public void renameCADModel(String oldName,String newName){
        SolidModelManager solidModelManager = m_simulation.get(SolidModelManager.class);
        if (solidModelManager.has(oldName)){
            CadModel cadModel = ((CadModel) m_simulation.get(SolidModelManager.class).getObject(oldName));
            cadModel.setPresentationName(newName); 
        }
     }
     
     // This method renames a part 
     public void renamePart(String oldName,String newName){
        SimulationPartManager simulationPartManager = m_simulation.get(SimulationPartManager.class); 
        if (simulationPartManager.has(oldName)){
            GeometryPart part = m_simulation.get(SimulationPartManager.class).getPart(oldName);
            part.setPresentationName(newName); 
        }
     }
     
    // This method renames a part curve 
     public void renamePartCurve(String partName, String oldCurveName, String newCurveName) {
         CadPart cadPart_1 = ((CadPart) m_simulation.get(SimulationPartManager.class).getPart(partName));
         PartCurve partCurve_1 = cadPart_1.getPartCurveManager().getPartCurve(oldCurveName);
         partCurve_1.setPresentationName(newCurveName);
     }
     
     // This method renames a part surface 
     public void renamePartSurface(String partName,String oldName,String newName){
        SimulationPartManager simulationPartManager = m_simulation.get(SimulationPartManager.class); 
        if (simulationPartManager.has(partName)){ 
            GeometryPart part = m_simulation.get(SimulationPartManager.class).getPart(partName);
            Collection<PartSurface> partSurfaces = part.getPartSurfaces(); 
            for (PartSurface partSurface: partSurfaces){
                String partSurfaceName = partSurface.getPresentationName(); 
                if (partSurfaceName.equals(oldName)){
                    partSurface.setPresentationName(newName); 
                }
            }
        }
     }
     
    // This method renames a scene 
    public void renameScene(String oldName,String newName){
       SceneManager sceneManager = m_simulation.get(SceneManager.class);
       if (sceneManager.has(oldName)){
           Scene scene = m_simulation.getSceneManager().getScene(oldName);
           scene.setPresentationName(newName); 
       }
    }  
    
    // ---------------------- setting ------------------------------------------
    
    // This method set a specified boundary belonging to a region to "farfield" type
     public void setBoundaryConditionFreestream(String regionName, String boundaryName){
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
        boundary.setBoundaryType(FreeStreamBoundary.class);
        boundary.getConditions().get(FreeStreamOption.class).setSelected(FreeStreamOption.Type.MA_P_T);
//        boundary.getConditions().get(FreeStreamOption.class).setSelected(FreeStreamOption.Type.ALTITUDE_MA);
     }
     
     public void setBoundaryConditionWall(String regionName, String boundaryName){
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
        boundary.setBoundaryType(WallBoundary.class);
     }
     
     public void setBoundaryConditionSymmetry(String regionName, String boundaryName){
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
        boundary.setBoundaryType(SymmetryBoundary.class);
     }
     
    // This method sets the dynamic viscosity in the physics continuum 
     public void setFreestreamAltitude(String regionName, String boundaryName, double value){
       Region region = m_simulation.getRegionManager().getRegion(regionName);
       Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
       AltitudeProfile altitudeProfile = boundary.getValues().get(AltitudeProfile.class);
       altitudeProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(value);
    }
     
    // This method sets the dynamic viscosity in the physics continuum 
     public void setFreestreamDynamicViscosity(String physicsContinuumName, double value){
        PhysicsContinuum physicsContinuum = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
        SingleComponentGasModel singleComponentGasModel = physicsContinuum.getModelManager().getModel(SingleComponentGasModel.class);    
        Gas gas = ((Gas) singleComponentGasModel.getMaterial());
        ConstantMaterialPropertyMethod constantMaterialPropertyMethod = ((ConstantMaterialPropertyMethod) gas.getMaterialProperties().getMaterialProperty(DynamicViscosityProperty.class).getMethod());
        constantMaterialPropertyMethod.getQuantity().setValue(value);
    }
     
    // This method sets the freestream Mach number where boundary nam should be 
    // the farfield boundary
    public void setFreestreamMachNumber(String regionName, String boundaryName, double value){
       Region region = m_simulation.getRegionManager().getRegion(regionName);
       Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
       MachNumberProfile machNumberProfile = boundary.getValues().get(MachNumberProfile.class);
       machNumberProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(value);
    }
     
    // This method sets the freestream pressure in the initial conditions of 
    // the physics continuum and the boundary conditions on the farifield. Hence, 
    // the boundary name provided should correspond to the farfield boundary
    public void setFreestreamPressure(String physicsContinuumName, String regionName, String boundaryName, double value){
       PhysicsContinuum physicsContinuum = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
       // Initial conditions
       InitialPressureProfile initialPressureProfile = physicsContinuum.getInitialConditions().get(InitialPressureProfile.class);
       initialPressureProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(value);
       // Farfield boundary conditions (gauge pressure)
       Region region = m_simulation.getRegionManager().getRegion(regionName);
       Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
       StaticPressureProfile staticPressureProfile = boundary.getValues().get(StaticPressureProfile.class);
       staticPressureProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(value);
    }
     
    // This method sets the freestream temperature in the initial conditions of 
    // the physics continuum and the boundary conditions on the farifield. Hence, 
    // the boundary name provided should correspond to the farfield boundary
    public void setFreestreamTemperature(String physicsContinuumName, String regionName, String boundaryName, double value){
        PhysicsContinuum physicsContinuum = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
        // Initial conditions
        StaticTemperatureProfile staticTemperatureProfile = physicsContinuum.getInitialConditions().get(StaticTemperatureProfile.class);
        staticTemperatureProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(value);
        // Farfield boundary conditions
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
        StaticTemperatureProfile staticTemperatureProfile_1 = boundary.getValues().get(StaticTemperatureProfile.class);
        staticTemperatureProfile_1.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(value);
    }
     
    // This method sets the freestream velocity in the initial conditions of 
    // the physics continuum and the boundary conditions on the farifield. Hence, 
    // the boundary name provided should correspond to the farfield boundary. 
    // Note that this method assumes zero sideslip angle. 
    public void setFreestreamVelocity(  String physicsContinuumName, 
                                        String regionName,
                                        String boundaryName, 
                                        double[] velocityVector){
        PhysicsContinuum physicsContinuum = ((PhysicsContinuum) m_simulation.getContinuumManager().getContinuum(physicsContinuumName));
        // Set initial conditions 
        VelocityProfile velocityProfile = physicsContinuum.getInitialConditions().get(VelocityProfile.class);
        velocityProfile.getMethod(ConstantVectorProfileMethod.class).getQuantity().setComponents(velocityVector[0],
                                                                                                 velocityVector[1],
                                                                                                 velocityVector[2]);
        // Set farfield boundary conditions 
        Region region = m_simulation.getRegionManager().getRegion(regionName);
        Boundary boundary = region.getBoundaryManager().getBoundary(boundaryName);
        double velocityMagnitude = Math.sqrt(  Math.pow(velocityVector[0],2.)+
                                   Math.pow(velocityVector[1],2.)+
                                   Math.pow(velocityVector[2],2.));
        double s = 1./velocityMagnitude;
        FlowDirectionProfile flowDirectionProfile = boundary.getValues().get(FlowDirectionProfile.class);
        flowDirectionProfile.getMethod(ConstantVectorProfileMethod.class).getQuantity().setComponents(  s*velocityVector[0],
                                                                                                        s*velocityVector[1],
                                                                                                        s*velocityVector[2]);
    }   
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // This method creates a surface custom mesh control for parts-based meshing
    private void createSurfaceCustomMeshControl2D(String meshOperationName,String partName, String partSurfaceName){
        AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(meshOperationName));
        SurfaceCustomMeshControl surfaceCustomMeshControl = autoMeshOperation2d.getCustomMeshControls().createSurfaceControl();
        surfaceCustomMeshControl.getGeometryObjects().setQuery(null);
        MeshPart meshPart = ((MeshPart) m_simulation.get(SimulationPartManager.class).getPart(partName));
        PartSurface partSurface = ((PartSurface) meshPart.getPartSurfaceManager().getPartSurface(partSurfaceName));
        surfaceCustomMeshControl.getGeometryObjects().setObjects(partSurface);
        surfaceCustomMeshControl.setPresentationName(partSurfaceName);
    }
}