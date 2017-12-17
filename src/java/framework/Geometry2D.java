/*
 * The purpose of this class is to handle any operations related to the geometry
 * node in Star-CCM+
 */

package framework;

import java.util.ArrayList;
import java.util.Collection;
import star.base.neo.DoubleVector;
import star.base.neo.IntVector;
import star.base.neo.NeoObjectVector;
import star.cadmodeler.Body;
import star.cadmodeler.BodyNameRefManager;
import star.cadmodeler.CadModel;
import star.cadmodeler.CadModelCoordinate;
import star.cadmodeler.CanonicalSketchPlane;
import star.cadmodeler.ExtrusionMerge;
import star.cadmodeler.LineSketchPrimitive;
import star.cadmodeler.PointSketchPrimitive;
import star.cadmodeler.Sketch;
import star.cadmodeler.SplineSketchPrimitive;
import star.common.Coordinate;
import star.common.GeometryPart;
import star.common.LabCoordinateSystem;
import star.common.Part;
import star.common.PartCurve;
import star.common.PartSurface;
import star.common.Simulation;
import star.common.SimulationPartManager;
import star.common.Units;
import star.meshing.CadPart;
import star.meshing.MeshActionManager;
import star.meshing.MeshPart;
import star.meshing.MeshPartFactory;
import star.meshing.SimpleCylinderPart;
import star.meshing.TessellationDensityOption;
import star.meshing.TessellationParameters;

/**
 *
 * @author shb
 */
public class Geometry2D {
    
    // --- Properties 
    // m_<name> = member variable (only accessible inside class)
    final private Toolbox m_toolbox;  
    final private Simulation m_simulation;
    
    // - variables used for airfoil geometry - 
    private String m_airfoilCadName; 
    private String m_airfoilPartName;
    private String m_airfoilPartSurfaceName; 
    private String m_airfoilPartCurveName; 
    private String m_farfieldPartName;
    private String m_farfieldPartSurfaceBoundaryName; 
    private String m_farfieldPartSurfaceInteriorName; 
    private double m_farfieldRadius; 
    private String m_controlVolumePartName; 
    
    // --- Constructor 
    public Geometry2D(Simulation sim) { 
        m_simulation = sim; 
        m_toolbox = new Toolbox(sim);  
        resetDefaults(); 
    } 
    
    // --- Methods
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    // ---------------------- primary methods ----------------------------------
    
    // This method resets all defaults
    final public void resetDefaults(){ 
        // - variables used for airfoil geometry - 
        m_airfoilCadName = "Airfoil"; 
        m_airfoilPartName = "Airfoil";
        m_airfoilPartSurfaceName = "Airfoil"; 
        m_airfoilPartCurveName = "Airfoil"; 
        m_farfieldPartName = "Farfield";
        m_farfieldPartSurfaceBoundaryName = "FarField"; 
        m_farfieldPartSurfaceInteriorName = "Fluid"; 
        m_farfieldRadius = 100.0; // 30 airfoil chord lengths
        m_controlVolumePartName = "Domain";
    }
    
    // This method clears all existing geometry
    public void clear(){
        m_toolbox.deleteScenes();
        m_toolbox.deleteParts();
        m_toolbox.deleteCADModels(); 
    }
    
    // This method creates an airfoil using the 3D CAD tools. Specifically, it  
    // reads in airfoil coordinates from the specified CSV file, creates a 
    // spline from the coordinates, and extrudes it one unit. The result is then
    // subtracted it from a cylindrical volume representing the domain, in order
    // to create the fluid control volume. Additional steps are applied to name
    // the various surfaces of the control volume (farfield, airfoil, TE). The 
    // final 3D geometry can then be converted to 2D using methods in the Mesher
    // class. 
    // CSVfilepath = airfoil coordinates file path (e.g. "/Users/Steven/XY.csv")
    public void createAirfoil(String CSVfilepath){
        
        // Get airfoil coordinates from file 
//        DoubleVector XY = getCSVAirfoilCoordinates(CSVfilepath);
        
        // Create the CAD model, which is an extruded airfoil (i.e. wing) with 
        // one unit depth. The key is that one end of the wing lies within the 
        // XY symmetry plane, which is the requirement for converting to 2D 
        // during meshing. 
        CadModel cadModel = m_toolbox.createCADModel(m_airfoilCadName);
//        Sketch sketch = createAirfoilSketch(cadModel,XY);
        cadModel.getFeatureManager().create2DSketches(CSVfilepath, true, true);
        Sketch sketch = ((Sketch) cadModel.getFeature("Sketch 1"));
        createAirfoilPartFromCAD(cadModel,sketch,m_airfoilPartName,
                          m_airfoilPartSurfaceName,
                          m_airfoilPartCurveName);
        
        // Next, let's create the farfield domain. Specifically, we generate 
        // a cylinder, which will become a circular domain when projected on 
        // the XY plane in 2D. 
        createDomain(m_farfieldRadius,m_farfieldPartName,
                     m_farfieldPartSurfaceBoundaryName,
                     m_farfieldPartSurfaceInteriorName);
        
        // Finally, we now create the control volume using boolean subtraction. 
        // Specifically, the volume occupied by the extruded airfoil is
        // subtracted from the volume occupied by the domain; what's left is the
        // volume of air over which we wish to solve the Navier-Stokes equations.
        createControlVolume(m_airfoilPartName, 
                            m_farfieldPartName, 
                            m_controlVolumePartName);
    }
    
    // ---------------------- secondary methods (settings) ---------------------
    
    // This method sets (gets) ...  
    public void setAirfoilCadName(String s){m_airfoilCadName = s;}
    public String getAirfoilCadName(){return m_airfoilCadName;}
    
    public void setAirfoilPartName(String s){m_airfoilPartName = s;}
    public String getAirfoilPartName(){return m_airfoilPartName;}
    
    // This method sets (gets) ...  
    public void setAirfoilPartSurfaceName(String s){m_airfoilPartSurfaceName = s;}
    public String getAirfoilPartSurfaceName(){return m_airfoilPartSurfaceName;}
    
    // This method sets (gets) ...  
    public void setAirfoilPartCurveName(String s){m_airfoilPartCurveName = s;}
    public String getAirfoilPartCurveName(){return m_airfoilPartCurveName;}
    
    // This method sets (gets) ...  
    public void setFarfieldPartName(String s){m_farfieldPartName = s;}
    public String getFarfieldPartName(){return m_farfieldPartName;}
    
    // This method sets (gets) ...  
    public void setFarfieldPartSurfaceName(String s){m_farfieldPartSurfaceBoundaryName = s;}
    public String getFarfieldPartSurfaceName(){return m_farfieldPartSurfaceBoundaryName;}
    
    // This method sets (gets) ...  
    public void setDomainPartSurfaceInteriorName(String s){m_farfieldPartSurfaceInteriorName = s;}
    public String getDomainPartSurfaceInteriorName(){return m_farfieldPartSurfaceInteriorName;}
    
    // This method sets (gets) ...  
    public void setDomainPartName(String s){m_controlVolumePartName = s;}
    public String getDomainPartName(){return m_controlVolumePartName;}
    
    // This method sets (gets) ...  
    public void setDomainExtentRadius(double d){m_farfieldRadius = d;}
    public double getDomainExtentRadius(){return m_farfieldRadius;}
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
    
    // ---------------------- 2D airfoil simulation ----------------------------
 
    // This method extrudes the airfoi 1 unit along Z-axis to create a part that 
    // can be used for CFD. This code snipet was reccorded in Starccm and modified
    private void createAirfoilPartFromCAD( CadModel cadModel,
                                    Sketch sketch,
                                    String partName, 
                                    String partSurfaceName,
                                    String partCurveName){
        Units units = m_simulation.getUnitsManager().getPreferredUnits(new IntVector(new int[] {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        ExtrusionMerge extrusionMerge = cadModel.getFeatureManager().createExtrusionMerge(sketch);
        extrusionMerge.setDirectionOption(0);
        extrusionMerge.setExtrudedBodyTypeOption(0);
        extrusionMerge.getDistance().setValue(1.0);
        extrusionMerge.setDistanceOption(0);
        extrusionMerge.setCoordinateSystemOption(0);
        extrusionMerge.setDraftOption(0);
        LabCoordinateSystem labCoordinateSystem = m_simulation.getCoordinateSystemManager().getLabCoordinateSystem();
        extrusionMerge.setImportedCoordinateSystem(labCoordinateSystem);
        CadModelCoordinate cadModelCoordinate = extrusionMerge.getDirectionAxis();
        cadModelCoordinate.setCoordinateSystem(labCoordinateSystem);
        cadModelCoordinate.setCoordinate(units, units, units, new DoubleVector(new double[] {0.0, 0.0, 1.0}));
        extrusionMerge.setFace(null);
        extrusionMerge.setBody(null);
        extrusionMerge.setSketch(sketch);
        extrusionMerge.setPostOption(1);
        extrusionMerge.setExtrusionOption(0);
        BodyNameRefManager bodyNameRefManager = extrusionMerge.getInteractionBodies();
        bodyNameRefManager.setBodies(new NeoObjectVector(new Object[] {}));
        extrusionMerge.setInteractingSelectedBodies(false);
        extrusionMerge.markFeatureForEdit();
        cadModel.getFeatureManager().execute(extrusionMerge);
        SplineSketchPrimitive spline = ((SplineSketchPrimitive) sketch.getSketchPrimitive("Spline 1"));
        Body cadmodelerBody = ((star.cadmodeler.Body) extrusionMerge.getBody(spline));
        // --- settings recommended by Cd-Adapco support representative --------
        // Otherwise, airfoil will not be smooth. It will have slight corners 
        // which will result a series of unphysical shock waves in starccm+. 
        TessellationParameters tessellationParameters_0 = new TessellationParameters();
        tessellationParameters_0.setCurveChordTolerance(0.001);
        tessellationParameters_0.setCurveChordAngle(5.0);
        tessellationParameters_0.setMaxFacetWidth(1.0);
        tessellationParameters_0.setSurfacePlaneTolerance(0.03);
        tessellationParameters_0.setSurfacePlaneAngle(8.0);
        // ---------------------------------------------------------------------
        cadModel.createParts(new NeoObjectVector(new Object[] {cadmodelerBody}), "SharpEdges", 30.0, tessellationParameters_0, true, 1.0E-5);
        m_toolbox.renamePart("Body 1",partName);
        m_toolbox.renamePartSurface(partName,"Default",partSurfaceName);  
        m_toolbox.renamePartCurve(partName,"Default",partCurveName);
    }
    
    // This method identifies the TE surface separately from the airfoil surface 
    private void flagTrailingEdgeSurface(String partName, String partSurface){
        CadPart cadModelPart = ((CadPart) m_simulation.get(SimulationPartManager.class).getPart(partName));
        PartSurface partSurface_0 = ((PartSurface) cadModelPart.getPartSurfaceManager().getPartSurface(partSurface));
        PartCurve partCurve = cadModelPart.getPartCurveManager().getPartCurve(partName);
        cadModelPart.getPartSurfaceManager().splitPartSurfacesByPartCurves(new NeoObjectVector(new Object[] {partSurface_0}), new NeoObjectVector(new Object[] {partCurve}));
        PartSurface partSurface_1 = ((PartSurface) cadModelPart.getPartSurfaceManager().getPartSurface(partName + " 2"));
        PartSurface partSurface_2 = ((PartSurface) cadModelPart.getPartSurfaceManager().getPartSurface(partName + " 3"));
        cadModelPart.combinePartSurfaces(new NeoObjectVector(new Object[] {partSurface_0, partSurface_1, partSurface_2}));
        try {
            PartSurface partSurface_3 = ((PartSurface) cadModelPart.getPartSurfaceManager().getPartSurface(partName + " 4"));
            partSurface_3.setPresentationName("TE");
        }
        catch (Exception e) {}
    }
    
    // This method creates a cylinder
    private void createDomain(double radius, String partName,String boundaryName,String interiorName){
        Units units = m_simulation.getUnitsManager().getPreferredUnits(new IntVector(new int[] {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        MeshPartFactory meshPartFactory = m_simulation.get(MeshPartFactory.class);
        SimpleCylinderPart simpleCylinderPart = meshPartFactory.createNewCylinderPart(m_simulation.get(SimulationPartManager.class));
        simpleCylinderPart.setDoNotRetessellate(true);
        LabCoordinateSystem labCoordinateSystem = m_simulation.getCoordinateSystemManager().getLabCoordinateSystem();
        simpleCylinderPart.setCoordinateSystem(labCoordinateSystem);
        Coordinate coordinate_0 = simpleCylinderPart.getStartCoordinate();
        coordinate_0.setCoordinateSystem(labCoordinateSystem);
        coordinate_0.setCoordinate(units, units, units, new DoubleVector(new double[] {0.0, 0.0, 0.0}));
        Coordinate coordinate_1 = simpleCylinderPart.getEndCoordinate();
        coordinate_1.setCoordinateSystem(labCoordinateSystem);
        coordinate_1.setCoordinate(units, units, units, new DoubleVector(new double[] {0.0, 0.0, 1.0}));
        simpleCylinderPart.getRadius().setUnits(units);
        simpleCylinderPart.getRadius().setValue(radius);
        simpleCylinderPart.getTessellationDensityOption().setSelected(TessellationDensityOption.Type.FINE);
        simpleCylinderPart.rebuildSimpleShapePart();
        simpleCylinderPart.setDoNotRetessellate(false);
        simpleCylinderPart.setPresentationName(partName);
        
        // Name surfaces
        PartSurface partSurface_0 = ((PartSurface) simpleCylinderPart.getPartSurfaceManager().getPartSurface("Cylinder Surface"));
        simpleCylinderPart.getPartSurfaceManager().splitPartSurfacesByAngle(new NeoObjectVector(new Object[] {partSurface_0}), 89.0);
        PartSurface partSurface_1 = ((PartSurface) simpleCylinderPart.getPartSurfaceManager().getPartSurface("Cylinder Surface 2"));
        PartSurface partSurface_2 = ((PartSurface) simpleCylinderPart.getPartSurfaceManager().getPartSurface("Cylinder Surface 3"));
        partSurface_0.setPresentationName(boundaryName);
        partSurface_1.setPresentationName("Default");
        partSurface_2.setPresentationName(interiorName);
    }
    
     // This method subtracts the airfoil volume from the sphere 
     private void createControlVolume(String airfoilPartName, String farfieldPartName, String controlVolumePartName){
        MeshActionManager meshActionManager = m_simulation.get(MeshActionManager.class);
        GeometryPart solidModelPart = m_simulation.get(SimulationPartManager.class).getPart(airfoilPartName);
        SimpleCylinderPart simpleCylinderPart = ((SimpleCylinderPart) m_simulation.get(SimulationPartManager.class).getPart(farfieldPartName));
        MeshPart meshPart = meshActionManager.subtractParts(new NeoObjectVector(new Object[] {solidModelPart, simpleCylinderPart}), simpleCylinderPart, "CAD");
        meshPart.setPresentationName(controlVolumePartName);
     }
    
}
