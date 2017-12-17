/*
 * This method can be used to setup and control a polygonal mesher for 2D flow. 
 * Prism layers will be create provided methods are called with appropriate 
 * options. 
 */

package framework;

import star.base.neo.NeoObjectVector;
import star.base.neo.StringVector;
import star.common.Simulation;
import star.common.SimulationPartManager;
import star.meshing.AutoMeshOperation2d;
import star.meshing.GenericRelativeSize;
import star.meshing.MeshOperationManager;
import star.meshing.MeshPart;
import star.prismmesher.PrismAutoMesher;
import star.prismmesher.PrismStretchingOption;
import star.prismmesher.PrismThickness;

/**
 *
 * @author shb
 */
public class PolygonalMesher extends Mesher {
    
    // -------------------------------------------------------------------------
    // ---------------------- A T T R I B U T E S ------------------------------
    // -------------------------------------------------------------------------
    
    final private Toolbox m_toolbox;    
    final private Simulation m_simulation;
    
    // -------------------------------------------------------------------------
    // ---------------------- C O N S T R U C T O R ----------------------------
    // -------------------------------------------------------------------------
    
    public PolygonalMesher(Simulation sim) { 
        super(sim); 
        m_simulation = sim; 
        m_toolbox = new Toolbox(sim); 
    }
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    // This method takes the geometry part representing the domain and assigns 
    // it to a fluid region to be discretized via meshing.  
    public void assignPartsToRegion(){
        m_toolbox.createBadgeFor2DMeshing(m_domainPartName);
        m_toolbox.createRegion(m_domainPartName,m_fluidRegionName);
    }
    
    // This method creates a mesh operation that will then be used to generate 
    // the mesh. 
    public void createMeshOperation(boolean isInviscid){
        // Get the part that is to be used for parts-based meshing (i.e. domain)
        MeshPart meshPart = ((MeshPart) m_simulation.get(SimulationPartManager.class).getPart(m_domainPartName));

        // Select mesher options 
        String[] meshOptions;
        if (isInviscid){
            meshOptions = new String[1];  
            meshOptions[0] = "star.twodmesher.DualAutoMesher2d";
        }
        else {
            meshOptions = new String[2]; 
            meshOptions[0] = "star.twodmesher.DualAutoMesher2d";
            meshOptions[1] = "star.prismmesher.PrismAutoMesher";
        }

        // Create the mesh operation
        MeshOperationManager meshOperationManager = m_simulation.get(MeshOperationManager.class); 
        if (meshOperationManager.has(m_meshOperationName)){
               AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));
               autoMeshOperation2d.getMeshers().setMeshersByNames(new StringVector(meshOptions));
        }
        else {
            AutoMeshOperation2d autoMeshOperation = meshOperationManager.createAutoMeshOperation2d(new StringVector(meshOptions), new NeoObjectVector(new Object[] {meshPart}));
            autoMeshOperation.setPresentationName(m_meshOperationName);
        }

        // For prism layer, specify the "wall thickness" option. In addition,
        // make sure to decrease the default total prism layer thickness from
        // it's default value of 0.33 meters. If the airfoil is 1m and one
        // wishes to customize the total thickness to no more than 0.02m by
        // using "surface custom controls", it won't work. This is because the
        // default settings state that anything less 10% of the total thickness,
        // hence 0.033 meters in this case, will not be generated. Hence, you'd
        // end up with no prism layer at all.
        if (!isInviscid){
            double defaultPrismLayerThickness = 0.01;
            AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));
            PrismAutoMesher prismAutoMesher = ((PrismAutoMesher) autoMeshOperation2d.getMeshers().getObject("Prism Layer Mesher"));
            prismAutoMesher.getPrismStretchingOption().setSelected(PrismStretchingOption.Type.WALL_THICKNESS);
            AutoMeshOperation2d autoMeshOperation2d_3 = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));
            PrismThickness prismThickness_0 = autoMeshOperation2d_3.getDefaultValues().get(PrismThickness.class);
            prismThickness_0.setRelativeSizeValue(defaultPrismLayerThickness);
        }
    }
    
    // This method customizes the mesh by changing default controls 
    public void customizeMesh(String surfaceName, String controlType, double meshParameterValue){
        switch (controlType){
            case "Target Surface Size": 
                m_toolbox.customizeTargetSurfaceSize2D(m_meshOperationName,m_domainPartName,surfaceName,meshParameterValue); 
                break; 
            case "Minimum Surface Size": 
                m_toolbox.customizeMinimumSurfaceSize2D(m_meshOperationName,m_domainPartName,surfaceName,meshParameterValue); 
                break; 
            case "Number of Prism Layers":
                int numberLayers = (int) meshParameterValue; 
                if (numberLayers == 0){m_toolbox.disablePrismLayer2D(m_meshOperationName,m_domainPartName,surfaceName);}
                else {m_toolbox.customizePrismLayerNumber2D(m_meshOperationName,m_domainPartName,surfaceName,numberLayers);} 
                break; 
            case "Prism Layer Near Wall Thickness":
                m_toolbox.customizePrismLayerWallThickness2D(m_meshOperationName,m_domainPartName,surfaceName,meshParameterValue);
                break;
            case "Prism Layer Total Thickness":
                m_toolbox.customizePrismLayerTotalThickness2D(m_meshOperationName,m_domainPartName,surfaceName,meshParameterValue);
                break; 
            case "Prism Layer Default Relative Thickness":
                AutoMeshOperation2d autoMeshOperation2d = ((AutoMeshOperation2d) m_simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));
                PrismAutoMesher prismAutoMesher = ((PrismAutoMesher) autoMeshOperation2d.getMeshers().getObject("Prism Layer Mesher"));
                prismAutoMesher.getPrismStretchingOption().setSelected(PrismStretchingOption.Type.WALL_THICKNESS);
                PrismThickness prismThickness = autoMeshOperation2d.getDefaultValues().get(PrismThickness.class);
                GenericRelativeSize genericRelativeSize = ((GenericRelativeSize) prismThickness.getRelativeSize());
                genericRelativeSize.setPercentage(meshParameterValue);
                break;
        }
    }
}
