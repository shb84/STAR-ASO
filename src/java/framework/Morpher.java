package framework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import star.base.neo.NeoObjectVector;
import star.common.Boundary;
import star.common.FileTable;
import star.common.PointSet;
import star.common.PointSetManager;
import star.common.Region;
import star.common.Simulation;
import star.common.SolverManager;
import star.common.TableManager;
import star.common.XyzTabularVectorProfileMethod;
//import star.morpher.ControlPointRegion; // starccm+ version 11.04.12
//import star.morpher.ControlPointRegionManager; // starccm+ version 11.04.12
import star.morpher.MeshDeformationProfile;
import star.morpher.MeshDeformationSolver;
import star.morpher.MorpherSpecification;

/**
 *
 * @author shb
 */
public class Morpher{

    // --- Properties 
    // m_<name> = member variable (only accessible inside class)
    final private Toolbox m_toolbox;
    final private Simulation m_simulation;
    private String m_controlVolumeRegionName;
    private String m_physicsContinuumName;
    private ArrayList<String> m_controlPointObjectNames;
    private ArrayList<String> m_controlPointFileNames;
    private ArrayList<String> m_adjointCostFunctionNames;
    private ArrayList<String> m_morpherFloatingBoundaryNames;

    // --- Constructor 
    public Morpher(Simulation sim) {
        m_simulation = sim;
        m_toolbox = new Toolbox(sim);
        setDefaults();
    }

    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C    M E T H O D S ---------------------
    // -------------------------------------------------------------------------

    // ---------------------- primary methods ----------------------------------

    // This method clears the previous setup. Specifically, it deletes the 
    // control points, control point tables, cost functions, gradient tables
    public void clear(){
        m_toolbox.deleteControlPoints();
        m_toolbox.deleteTables();
    }

    // This method resets all defaults.   
    final public void setDefaults(){
        m_controlVolumeRegionName = "Fluid";
        m_physicsContinuumName = "Physics";
        m_controlPointObjectNames = new ArrayList();
        m_controlPointFileNames = new ArrayList();
        m_adjointCostFunctionNames = new ArrayList();
        m_morpherFloatingBoundaryNames = new ArrayList();
    }

    // This method reads in the initial control point and creates the control 
    // points, control point tables, cost functions, and gradient tables 
    public void initialize(ArrayList<String> controlPointFileNames,
                           ArrayList<String> adjointCostFunctionReportNames,
                           ArrayList<String> morpherFloatingBoundaryNames){
        m_controlPointFileNames = controlPointFileNames;
        m_adjointCostFunctionNames = adjointCostFunctionReportNames;
        m_morpherFloatingBoundaryNames = morpherFloatingBoundaryNames;
        m_controlPointObjectNames = createControlPointObjects(m_controlPointFileNames);
        createGradientTables(m_adjointCostFunctionNames,m_controlPointObjectNames);
        for (String boundaryName: m_morpherFloatingBoundaryNames){
            setBoundaryCondition(boundaryName,"Floating");
        }
    }

    // This method sets the morpher boundary condition
    public void setBoundaryCondition(String boundaryName, String type) {
        Region region_0 = m_simulation.getRegionManager().getRegion(m_controlVolumeRegionName);
        Boundary boundary_0 = region_0.getBoundaryManager().getBoundary(boundaryName);
        switch (type){
            case "Floating":
                boundary_0.getConditions().get(MorpherSpecification.class).setSelected(MorpherSpecification.Type.FLOATING);
                break;
            case "Fixed":
                boundary_0.getConditions().get(MorpherSpecification.class).setSelected(MorpherSpecification.Type.FIXED);
                break;
            case "Displacement":
                boundary_0.getConditions().get(MorpherSpecification.class).setSelected(MorpherSpecification.Type.DISPLACEMENT);
                break;
        }
    }

//    // This method updates a specified control point table
//    public void updateControlPointTables(ArrayList<String> controlPointTableNames){
//        TableManager tableManager = m_simulation.getTableManager(); 
//        for (String tableName: controlPointTableNames){
//            FileTable table = ((FileTable) tableManager.getTable(tableName));
//            table.extract();
//        }
//    }

    // This method updates the control points and deforms the mesh 
    public void morph(){

       SolverManager solverManager;
       MeshDeformationSolver meshDeformationSolver;

       solverManager = m_simulation.getSolverManager();
       meshDeformationSolver = (MeshDeformationSolver) solverManager.getSolver(MeshDeformationSolver.class);

       meshDeformationSolver.deformMesh();
    }

    // This method updates the CSV file linked to the "control point table" 
    // in Starccm+, which contains the control point XYZ coordinates. Each 
    // design variable object in the list contains information about what 
    // table in the starccm+ model it is linked to and what (row,col) it's 
    // value represents. Each starccm+ "file table" is linked to a specific 
    // CSV file outside of Starccm+. It is by updating this file and re-
    // importing it that the Starccm tables get updated. I couldn't not find a
    // way to change the table values directly, without going through an 
    // external CSV file. Perhaps something to consider in future updates. 
    public void update(ArrayList<DesignVariable> designvars){

        // Get table manager  
        TableManager tableManager = m_simulation.getTableManager();

        // Loop over each control point in list 
        for (DesignVariable controlPoint: designvars)
        {

            // Extract the properties contained in each DesignVariable object
            int controlPointID              = controlPoint.getID();
            String controlPointTableName    = controlPoint.getControlPointTableID();
            double currentValue             = controlPoint.getCurrentValue();
            double initialValue             = controlPoint.getInitialValue();
            int row                         = controlPoint.getControlPointTableRow();
            int col                         = controlPoint.getControlPointTableCol();

            // Not all design variables are control points. They could also be 
            // boundary parameters, such as angle of attack (alpha). We'll use 
            // an "if statement" to filter by key word: 
            if (!controlPoint.getVarName().equals("alpha")){
                // Get the current value in the table
                FileTable table = ((FileTable) tableManager.getTable(controlPointTableName));

                // In case file no longer exists locally, recreate it
                String path = m_simulation.getSessionDir()+File.separator+controlPointTableName+".csv";
                table.export(path,",");
                table.setFileName(controlPointTableName+".csv");

                // The FileTable is linked to a CSV file containing the current 
                // control point XYZ coordinates. We will update it using a 
                // CSVFile object, which contains methods for doing just that. 
                // Important: Starccm+ updating is cumulative. This means that the 
                // dx dy dz are the cumulative deltas for all optimizer iterations 
                // up to now. 
                CSVFile csv = new CSVFile(path);

                // double cumulativeDelta = currentValue - initialValue;

                try {
                   // Update CSV file
                   // Note: the columns of the ControlPointTable are assumed to be
                    //  col  0  1  2 3 4 5
                   //       dx dy dz x y z --> hence the "col-3"
                   // Also, the CSV file has a header row --> hence the "row + 1"
                   // csv.update(cumulativeDelta,row+1,col-3);
                   csv.update(currentValue,row+1,col-3);

                   m_toolbox.print("Design Variable " + controlPointID + " = " + currentValue);

                   // Reload CSV file
                   table.extract();
                }
                catch (IOException e) {
                    m_toolbox.print("Design variable "+ controlPointID );
                    m_toolbox.print("Morpher.class: updateControlPointTable(): "+ e.getMessage());
                }
            }
        }
    }

    // ---------------------- secondary methods --------------------------------

    // This method sets (gets) ...
    public void setPhysicsContinuumName(String s){m_physicsContinuumName = s;}
    public String getPhysicsContinuumName(){return m_physicsContinuumName;}

    // This method sets (gets) ...
    public void setControlVolumeRegionName(String s){m_controlVolumeRegionName = s;}
    public String getControlVolumeRegionName(){return m_controlVolumeRegionName;}

    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E    M E T H O D S -------------------
    // -------------------------------------------------------------------------

    // This method creates a file table
    private FileTable createFileTable(String CSVfilepath,String tableName){
        TableManager tableManager = m_simulation.getTableManager();
        FileTable fileTable;
       if (tableManager.has(tableName)){
           fileTable = (FileTable) tableManager.getTable(tableName);
           fileTable.extract();
       }
       else {
           fileTable = (FileTable) tableManager.createFromFile(CSVfilepath);
           fileTable.setPresentationName(tableName);
       }
        return fileTable;
    }

    // This method creates control point tables from a specified CSV file list.
    private ArrayList<String> createControlPointObjects(ArrayList<String> controlPointFileNames){
        ArrayList<String> controlPointNames = new ArrayList();
        for (String controlPointFileName: controlPointFileNames){
            String filepath = m_simulation.getSessionDir() + File.separator + controlPointFileName;
            String controlPointTableName = controlPointFileName.split("[.]")[0];
            File file = new File(filepath);
            if (file.exists()){
               createControlPointObject(filepath,controlPointTableName);
               controlPointNames.add(controlPointTableName);
            }
        }
        return controlPointNames;
    }

    // This method creates cost function gradient tables. For each cost function
    // in the list, there must be an associated control point object. Hence, the
    // lists must have equal lengths.
    private void createGradientTables(ArrayList<String> adjointCostFunctionNames,
                                      ArrayList<String> controlPointObjectNames){
        for (String adjointCostFunctionName: adjointCostFunctionNames){
            m_toolbox.createGradientTable(adjointCostFunctionName, controlPointObjectNames);
        }
    }

    // This method creates a table of control points to be used for mesh deformation
    public void createControlPointObject(String CSVfilepath, String tableName) {
        File csvfile = new File(CSVfilepath);
        if (csvfile.exists()){

            // First, let's create a table associated with the CSVfilepath
            FileTable fileTable = createFileTable(CSVfilepath, tableName);

            // However, we don't want to over-write the CSV file directly. So
            // let's make a local copy.
            String path = fileTable.getFile().getPath();
            String copy = CSVfilepath.split("[.]")[0] + "_mod.csv";
            Path source = Paths.get(path);
            Path target = Paths.get(copy);
            try {Files.delete(target);}
            catch (IOException e) {m_toolbox.print(e.getMessage());}
            try {Files.copy(source,target);}
            catch (IOException e) {m_toolbox.print(e.getMessage());}
            fileTable.setFileName(copy);

            // We can now create the control points
            // ------------------- starccm+ v11.04.12 --------------------------
            PointSetManager pointSetManager = m_simulation.get(PointSetManager.class);
            if (pointSetManager.has(tableName)){
                    PointSet pointSet = ((PointSet) pointSetManager.getObject(tableName));
                    pointSetManager.removePointSet(pointSet);
            }
            PointSet pointSet = pointSetManager.createTablePointSet(tableName, fileTable, "X", "Y", "Z");
            // -----------------------------------------------------------------
//            PointSetManager controlPointRegionManager = m_simulation.get(PointSetManager.class);
//            if (controlPointRegionManager.has(tableName)){
//                    PointSet controlPointRegion = ((PointSet) controlPointRegionManager.getObject(tableName));
//                    controlPointRegionManager.removePointSet(controlPointRegion);
//            }
//            PointSet controlPointRegion = controlPointRegionManager.createTablePointSet(tableName, fileTable, "X", "Y", "Z");

            // Finally, enable mesh deformation based on table values
            MeshDeformationProfile meshDeformationProfile_1 = pointSet.getValues().get(MeshDeformationProfile.class);
            meshDeformationProfile_1.setMethod(XyzTabularVectorProfileMethod.class);
            meshDeformationProfile_1.getMethod(XyzTabularVectorProfileMethod.class).setTable(fileTable);
            meshDeformationProfile_1.getMethod(XyzTabularVectorProfileMethod.class).setXData("dX");
            meshDeformationProfile_1.getMethod(XyzTabularVectorProfileMethod.class).setYData("dY");
            meshDeformationProfile_1.getMethod(XyzTabularVectorProfileMethod.class).setZData("dZ");
        }
        else {
            m_toolbox.print("Control points not created. File not found: " + CSVfilepath);
        }
    }
}
