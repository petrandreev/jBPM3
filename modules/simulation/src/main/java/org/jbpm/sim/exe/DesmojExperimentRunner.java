package org.jbpm.sim.exe;

import java.util.Map;

import org.jbpm.sim.def.JbpmSimulationModel;

import desmoj.core.simulator.SimTime;
import desmoj.core.util.SimRunListener;
import desmoj.extensions.experimentation.ui.GraphicalObserverContext;
import desmoj.extensions.experimentation.ui.TimeSeriesPlotter;
import desmoj.extensions.experimentation.util.AccessUtil;
import desmoj.extensions.experimentation.util.ExperimentRunner;

public class DesmojExperimentRunner extends ExperimentRunner {
  
  public DesmojExperimentRunner() {
    super();
  }
  
  public DesmojExperimentRunner(JbpmSimulationModel m) {
    super(m);
  }
  
  public SimRunListener[] createSimRunListeners(GraphicalObserverContext c) {
    JbpmSimulationModel model = (JbpmSimulationModel)getModel();
    
    if (model.getResourceTimeSeries()!=null && model.getResourceTimeSeries().length>0) {
      TimeSeriesPlotter tp1 = new TimeSeriesPlotter("Resources",c, model.getResourceTimeSeries(), 360,360);

//    HistogramPlotter hp = new HistogramPlotter("Waiting for Task", c, model.getEntityWaitTimeHistogram(""),"h", 360,360, 365,0);
      return new SimRunListener[] {tp1};
    }
    return new SimRunListener[0];
  }
  
  public Map createParameters() {
    Map pm = super.createParameters();
    AccessUtil.setValue(pm, "stopTime", new SimTime(2800.0));
    return pm;
  }
}
