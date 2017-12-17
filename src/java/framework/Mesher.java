/*
 * This is a super class intended to be extended into subclass corresponding 
 * to different mesh types (e.g. trimmer, polyhedral, polygonal, etc.)
 */

package framework;

import star.common.*;
import star.meshing.*;

/**
 *
 * @author shb
 */
public class Mesher {
    
    // -------------------------------------------------------------------------
    // ---------------------- A T T R I B U T E S ------------------------------
    // -------------------------------------------------------------------------
    
    final private Toolbox m_toolbox;    
    final private Simulation m_simulation;
    protected String m_meshOperationName;
    protected String m_domainPartName; 
    protected String m_fluidRegionName;
    
    // -------------------------------------------------------------------------
    // ---------------------- C O N S T R U C T O R ----------------------------
    // -------------------------------------------------------------------------
    
    public Mesher(Simulation sim) { 
        m_simulation = sim; 
        m_toolbox = new Toolbox(sim); 
        m_meshOperationName = "Automated Mesh"; 
        m_domainPartName = "Domain"; 
        m_fluidRegionName = "Fluid";
    }
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    // This method clears all existing meshes and meshers
    public void clear(){
        m_toolbox.deleteMeshes();
        m_toolbox.deleteMeshers();
        m_toolbox.deleteRegions(); 
    }
 
    // This method generates the mesh using parts-based meshing 
    public void mesh(){
        AutoMeshOperation autoMeshOperation = ((AutoMeshOperation) m_simulation.get(MeshOperationManager.class).getObject(m_meshOperationName));
        autoMeshOperation.execute();
    }
 
    // This method sets (gets) the control volume part name  
    public void setDomainPartName(String s){m_domainPartName = s;}
    public String getDomainPartName(){return m_domainPartName;}
    
    // This method sets (gets) the control volume region name
    public void setFluidRegionName(String s){m_fluidRegionName = s;}
    public String getControlVolumeRegionName(){return m_fluidRegionName;}
    
    // This method sets (gets) the meshOperationName 
    public void setMeshOperationName(String s){m_meshOperationName = s;}
    public String getMeshOperationName(){return m_meshOperationName;}
     
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
}
