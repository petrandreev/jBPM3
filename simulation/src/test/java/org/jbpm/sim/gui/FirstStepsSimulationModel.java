package org.jbpm.sim.gui;

import org.jbpm.sim.def.DefaultJbpmSimulationModel;
import org.jbpm.sim.exe.DesmojExperimentRunner;

import desmoj.extensions.experimentation.ui.ExperimentStarterApplication;

public class FirstStepsSimulationModel extends DefaultJbpmSimulationModel {
  
  public FirstStepsSimulationModel() {
    super(getProcessXml());
  }
  public static String getProcessXml() {    
//     FirstStepsSimulationModel.class.getResourceAsStream("/FirstSteps/processdefinition.xml") ).toString();
    return     
      "<process-definition name='test' start-distribution='start new process instances of test'>" +

      "  <distribution name='start new process instances of test' sample-type='real' type='constant' value='20' /> " +
      "  <distribution name='time required for task one'          sample-type='real' type='normal'   mean='25' standardDeviation='10' /> " +      
      "  <distribution name='time required for task two'          sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      
      "  <distribution name='time required for automated state'   sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      

      "  <resource-pool name='tester'      pool-size='2' />" +      
      "  <resource-pool name='big machine' pool-size='3' />" +      
      "  <swimlane name='tester' pool-size='1' />" +

      "  <start-state name='start'>" +
      "    <transition to='task one' />" +
      "  </start-state>" +
      
      "  <task-node name='task one'>" +
      "    <task swimlane='tester' time-distribution='time required for task one' />" +
      "    <transition to='task two' />" +
      "  </task-node>" +

      "  <task-node name='task two'>" +
      "    <task swimlane='tester' time-distribution='time required for task two' />" +
      "    <transition to='automated state' />" +
      "  </task-node>" +

      "  <state name='automated state' time-distribution='time required for automated state'>" +
      "    <resource-needed pool='big machine' amount='2' />" +
      "    <transition to='end' />" +
      "  </state>" +

      "  <end-state name='end'/>" +

      "</process-definition>" ;
  }
  
  public static void main(String[] args) throws Exception {
    new ExperimentStarterApplication(FirstStepsSimulationModel.class, DesmojExperimentRunner.class).setVisible(true);
  }
  
}
