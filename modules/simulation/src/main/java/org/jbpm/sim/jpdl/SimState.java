package org.jbpm.sim.jpdl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.node.State;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.sim.action.StartWorkOnStateAndPlanCompletion;

public class SimState extends State {

  private static final long serialVersionUID = 1L;
  private static Log log = LogFactory.getLog(SimState.class);

  public void read(Element nodeElement, JpdlXmlReader jpdlXmlReader) {
    super.read(nodeElement, jpdlXmlReader);  
    
    // add event which listens to node enter and schedules state completion
    Event nodeEnterEvent = new Event(Event.EVENTTYPE_NODE_ENTER);    
    nodeEnterEvent.addAction(new StartWorkOnStateAndPlanCompletion());
    addEvent(nodeEnterEvent);
  }

}
