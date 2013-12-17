package org.jbpm.sim.gui;

import org.jbpm.sim.def.DefaultJbpmSimulationModel;
import org.jbpm.sim.exe.DesmojExperimentRunner;

import desmoj.extensions.experimentation.ui.ExperimentStarterApplication;

public class OneTaskSimulationModel extends DefaultJbpmSimulationModel {
  
  public OneTaskSimulationModel() {
    super(getProcessXml());
  }

  public static String getProcessXml() {    
    return
      "<process-definition>" +
      
      "  <swimlane name='tester' pool-size='1' />" +

      "  <start-state name='start'>" +
      "    <transition to='test' />" +
      "  </start-state>" +
      "  <task-node name='test'>" +
      "    <task swimlane='tester' />" +
      "    <transition to='end' />" +
      "  </task-node>" +
      "  <end-state name='end'/>" +

      "</process-definition>" ;
  }
  
  public static void main(String[] args) throws Exception {
    new ExperimentStarterApplication(OneTaskSimulationModel.class, DesmojExperimentRunner.class).setVisible(true);
  }
  
}
