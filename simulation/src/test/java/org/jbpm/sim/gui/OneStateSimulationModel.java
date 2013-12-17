package org.jbpm.sim.gui;

import org.jbpm.sim.def.DefaultJbpmSimulationModel;
import org.jbpm.sim.exe.DesmojExperimentRunner;

import desmoj.extensions.experimentation.ui.ExperimentStarterApplication;

public class OneStateSimulationModel extends DefaultJbpmSimulationModel {
  
  public OneStateSimulationModel() {
    super(getProcessXml());
  }
  public static String getProcessXml() {    
    return
    "<process-definition>" +
    "  <start-state name='a'>" +
    "    <transition to='b'/>" +
    "    <simulation avg-duration='5' signal='straight ahead' />" +
    "  </start-state>" +
    "  <state name='b'>" +
    "    <transition to='end'/>" +
    "    <simulation avg-duration='2' signal='turn left here' />" +
    "  </state>" +
    "  <end-state name='end'/>" +
    "</process-definition>";
  }
  
  public static void main(String[] args) throws Exception {
    new ExperimentStarterApplication(OneStateSimulationModel.class, DesmojExperimentRunner.class).setVisible(true);
  }
  
}
