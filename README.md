Description: STAR-CCM+ - Aerodynamic Shape Optimization (STAR-ASO) (see wiki for more)

**Rule: please clone repo or create a new branch before editing. Don't work in master!**

Contents: 

* **jar** <--- contains "zipped" up java packages (needed when running on cluster) 
   * ASO.jar <--- files contained in src/java (should be updated whenever "src/java/..." changes)
   * commons-math3-3.6.1.jar 
   * opencsv_3_9.jar
* **rae** <--- some simple airfoil examples to get you started 
  * _RAE2822-Adapt-Example.zip_
  * _RAE2822-Create-Example.zip_
  * _RAE2822-Optimizer-Example.zip_
  * _RAE2822-DOE-Example.zip_
* **src** <--- contains all source code 
   * java 
      * framework <--- user classes to automate things in STAR-CCM+
         * _Airfoil2D.java_
         * _Blackbox.java_
         * _CFDModel.java_
         * _CST.java_
         * _CSVFile.java_
         * _DesignVariable.java_
         * _FlightCondition.java_
         * _Geometry2D.java_
         * _MeshAdaptation.java_
         * _Mesher.java_
         * _Morpher.java_
         * _PolygonalMesher.java_
         * _Problem.java_
         * _Solver.java_
         * _Toolbox.java_
      * macros <--- main class (which uses methods from framework classes) that drives STAR-CCM+ 
         * _AdaptMesh.java_
         * _CreateAndRunAirfoil.java_
         * _RunOptimizationMacro.java_
         * _SetFlightConditionsAndRun.java_
   * pbs <--- pre-made PBS scripts to run on the cluster 
     * _SubmitJob_Adapt.pbs_
     * _SubmitJob_Create.pbs_
     * _SubmitJob_DOE.pbs_
     * _SubmitJob_Optimize.pbs_
   * python <--- scripts that call STAR-CCM+ macros to do achieve some broader goal (e.g. optimization) 
     * _RunASO.py_
     * _RunDOE.py_
